package interfaceApplication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InitialContext;

import org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.omg.CORBA.StringHolder;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.sun.star.beans.StringPair;
import com.sun.star.uno.RuntimeException;

import Concurrency.distributedLocker;
import JGrapeSystem.numberHelper;
import JGrapeSystem.rMsg;
import apps.appIns;
import apps.appsProxy;
import database.db;
import httpClient.request;
import httpServer.grapeHttpUnit;
import interfaceController.interfaceUnit;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import interfaceType.apiType;
import interfaceType.apiType.tpye;
import nlogger.nlogger;
import offices.excelHelper;
import rpc.execRequest;
import security.codec;
import string.StringHelper;
import thread.ThreadEx;
import time.TimeHelper;
import unit.urlContent;

/*
 * 
 * */

public class task {
	public enum state {
	     enabled, disabled 
	}
	public enum runState {
	     running, idle, error 
	}
	private static AtomicBoolean stateRun;
	//private static Thread ticktockThread = null;
	private static HashMap<Integer, ScheduledExecutorService> ticktockThread = null;
	
	private static ExecutorService taskWorker;
	
	private GrapeTreeDBModel db;
	private String pkString;
	private static final String lockerName = "crawlerTask_Query_Locker";
	
	private String currentURL = "";
	
	private Pattern regx = Pattern.compile("\\[\\%\\%\\]");
	//private static final String RunnerlockerName = "crawlerTask_Running_Locker";
	static {
		stateRun = new AtomicBoolean(false);
		ticktockThread = new HashMap<>();
		taskWorker = Executors.newFixedThreadPool( 1 );
	}
	/**启动采集模块服务
	 * @return
	 */
	public String startService(){
		appIns apps = appsProxy.getCurrentAppInfo();
		if( apps != null && !ticktockThread.containsKey(apps.appid) ) {
			resetTaskState();
			ScheduledExecutorService serv = Executors.newSingleThreadScheduledExecutor();;
			distributedLocker servLocker = distributedLocker.newLocker(lockerName);
			if( servLocker.lock() ) {//判断是否锁定成功
				
				serv.scheduleAtFixedRate(() -> {
					//while(stateRun) {
					appsProxy.setCurrentAppInfo(apps);
					distributedLocker sLocker = new distributedLocker(lockerName);
					if( !sLocker.isExisting() ) {//锁不存在了，退出服务
						task t = new task();
						t.stopService();
					}
					
					appsProxy.proxyCall("/crawler/task/DelayBlock",apps);
						//分块方式获得数据表数据，并执行过滤，最后生成结果值 
					//}
				}, 0, 1, TimeUnit.MINUTES);
				
				ticktockThread.put(apps.appid, serv);
			}
		}
		return rMsg.netState(true);
	}
	private long resetTaskState() {
		JSONObject updateObj = new JSONObject();
		updateObj.puts("runstate", 0);
		return db.data(updateObj).updateAll();
	}
	
	/**获得当前模块任务状态
	 * @return
	 */
	public String queryService() {
		distributedLocker sLocker = new distributedLocker(lockerName);
		return rMsg.netState(sLocker.isExisting());
	}
	/**停止采集模块服务
	 * @return
	 */
	public String stopService() {
		appIns apps = appsProxy.getCurrentAppInfo();
		if( ticktockThread.containsKey(apps.appid) ) {
			resetTaskState();
			ScheduledExecutorService serv = ticktockThread.get(apps.appid);
			if( !serv.isTerminated()) {
				serv.shutdown();
			}
			ticktockThread.remove(apps.appid);
		}
		(new distributedLocker(lockerName)).releaseLocker();
		return rMsg.netState(true);
	}
	//遍历任务
	@SuppressWarnings("unchecked")
	public String DelayBlock() {
		if( stateRun.compareAndSet(false, true) ) {
			JSONArray taskList = db.scan( (array)->{//获得符合条件的任务列表
				JSONArray _array = new JSONArray();
				JSONObject json;
				for(Object obj : array) {
					json = (JSONObject)obj;
					if( json.getLong("state") == 1 ) {
						if( (json.getLong("neartime") + json.getLong("runtime") <= TimeHelper.nowMillis()) && json.getLong("runstate") == 0 ) {
							_array.add(obj);
						}
					}
				}
				return _array;
			}, 30);//同步函数
			
			if( taskList.size() > 0 ) {
				JSONObject json;
				for(Object obj : taskList) {
					json = (JSONObject)obj;
					db.or().eq(pkString, json.getString("_id"));
				}
				db.data(new JSONObject("runstate",1)).updateAll();//更新任务状态运行中
				for(Object obj : taskList) {
					json = (JSONObject)obj;
					appendTask(json);
				}
				
			}
			stateRun.set(false);
		}
		return "";
	}
	private void appendTask(JSONObject json) {
		appIns apps = appsProxy.getCurrentAppInfo();
		taskWorker.execute(()->{
			appsProxy.setCurrentAppInfo(apps);
			long taskrl = ( !taskRun(json) ) ? 2 : 0;
			_db().eq(pkString, json.getString("_id")).data( (new JSONObject("runstate",taskrl)).puts("neartime", TimeHelper.nowMillis() ) ).update();//更新任务状态为执行结果
		});
	}
	
	@SuppressWarnings("unchecked")
	private boolean catchData(String contentURL,JSONObject taskInfo,JSONObject postData) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		boolean rb = true;
		String html = "";
		if( postData != null ) {
			html = request.Post(contentURL, postData);
		}
		else {
			html = request.page(contentURL);
		}
		Document doc = Jsoup.parse(html) ;
		JSONArray dataBlock = taskInfo.getJsonArray("data");
		JSONObject block;
		JSONObject dataResult = new JSONObject();
		for(Object obj : dataBlock) {
			block = (JSONObject)obj;
			Object ro = dataSelecter( contentURL,doc, getSelecter(block.getString("selecter")),  block.getBoolean("isTEXT"));
			if( ro != null) {
				dataResult.put( block.getString("key") , ro );
			}
			else {
				break;
			}
		}
		//---------------投递采集来 的数据
		String collect = taskInfo.getString("collectApi");
		if( dataResult != null && dataResult.size() > 0 ) {
			if( StringHelper.InvaildString( collect ) ) {
				JSONObject postParam = new JSONObject("param",codec.encodeFastJSON( dataResult.toJSONString() ));
				appIns apps = appsProxy.getCurrentAppInfo();
				JSONObject rjson = JSONObject.toJSON( (String)appsProxy.proxyCall(collect,postParam,apps) );
				/*
				 * RPC返回对象里的 errorcode 不为0 时停止继续执行采集任务
				 * */
				if( rjson != null && rjson.containsKey("errorcode") ) {
					if( rjson.getInt("errorcode") != 0 ) {
						System.out.println("crawler system breaking by remoteSystem!");
						contentURL = null;
						return false;
					}
				}
				rb = true;
			}
			else {
				nlogger.logout("url" + contentURL + "  ->数据收集Api异常");
			}
		}
		return rb;
	}
	
	private boolean PageMode(String host, JSONObject initJson,JSONObject taskInfo) {
		boolean rb = true;
		urlContent contentUrlObj = getURL( host, initJson.getString("base"), initJson.getString("selecter") );
		if( contentUrlObj != null ) {
			String contentURL = contentUrlObj.getCur();//获得采集任务起始地址
			
			while( contentURL != null ) {
				try {
					currentURL = contentURL;
					//----------------确定循环数据
					JSONObject loopJson = taskInfo.getJson("loop");
					int loopMode = loopJson.getInt("mode");
					String loopURL = null;
					do {
						//---------------开始采集内容
						if( contentURL != null ) {
							if( catchData(contentURL,taskInfo,null ) == false ) {
								break;
							}
						}
						urlContent nextUrlObj;
						switch(numberHelper.number2int(loopMode) ) {
						case 1://通过起始页获得
							nextUrlObj = getURL( "", contentUrlObj.getUp(), loopJson.getString("selecter") );//获得下一页URL
							
							if( nextUrlObj != null ) {
								loopURL = nextUrlObj.getCur();
								contentUrlObj.setUp( nextUrlObj.getCur() );;
							}
							else {
								loopURL = null;
							}
							break;
						case 2://通过内容页获得
							nextUrlObj = getURL( "", contentURL, loopJson.getString("selecter") );//获得下一页URL
							loopURL = nextUrlObj != null ? nextUrlObj.getCur() : null;
							break;
						default:
							loopURL = null;
							break;
						}
						contentURL = loopURL;
					}while( contentURL != null);
					//-------------------------
				} catch (Exception e) {
					nlogger.login(e,"url" + contentURL + "  ->异常");
					//nlogger.logout(e);
					contentURL = null;
					e.printStackTrace();
					rb = false;
				}
			}
		}
		return rb;
	}
	
	public void updateURL(String host,int method, String runBase, JSONArray _aArray,int idx,JSONObject taskInfo) {
		if( idx >= _aArray.size()) {//到底了
			try {
				if( method == 1 ) {//POST请求
					catchData(host,taskInfo,JSONObject.toJSON(runBase) );
				}
				else {
					catchData(runBase,taskInfo, null );
				}
				//System.out.println(runBase);
				return;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		JSONObject json = (JSONObject)_aArray.get(idx);
		int init = json.getInt("init");
		int over = json.getInt("end");
		int step = json.containsKey("step") ? json.getInt("step") : 1;
		
		for(int s = init; s<=over; s=s+step ) {
			String tempBase = runBase;
			tempBase = tempBase.replaceFirst("\\[\\%\\%\\]", StringHelper.Any2String(s) );
			updateURL(host,method,tempBase,_aArray,idx + 1,taskInfo);
		}
	}
	
	private boolean URLMode(String host,JSONObject initJson,JSONObject taskInfo) {
		boolean rb = true;
		String base = initJson.getString("base");//主URL
		String sels = initJson.getString("selecter");
		int method = initJson.getInt("method");
		sels = sels.replaceAll(";", ",");
		JSONArray _aArray = JSONArray.toJSONArray( "[" + sels + "]" );
		String store = base;

		Matcher m = regx.matcher(store);
		int maxBlock =0;
		while( m.find() ) {
			maxBlock++;
		}
		if( maxBlock == _aArray.size() ) {
			String runBase = base;
			updateURL(host,method,runBase,_aArray,0,taskInfo);
		}
		else {
			nlogger.login("任务配置错误 url：" + store + " && 选择器组:" + sels + " 不匹配");
		}
		
		return rb;
	}
	
	/**执行任务
	 * @param taskBlock
	 * @return
	 */
	private boolean taskRun(JSONObject taskBlock) {
		boolean rb = false;
		
		JSONObject taskInfo = taskBlock.getJson("info");
		if( taskInfo != null ) {
			String host = taskInfo.getString("host");
			JSONObject initJson = taskInfo.getJson("init");
			
			int typeMode = numberHelper.number2int( initJson.getString("type"));
			switch(typeMode) {
			case 0:
				rb = PageMode(host,initJson,taskInfo);
				break;
			case 1:
				rb = URLMode(host,initJson,taskInfo);
			}
		}
		return rb;
	}
	
	private String safeHTML(Element node) {
		Elements script = node.select("script");
		if( script.size() > 0) {
			script.remove();
		}
		Elements a = node.select("a");
		a.attr("href", "javascript:;");
		return node.html();
	}
	
	/**选择器的值
	 * @param doc
	 * @param sel
	 * @return
	 */
	private Object dataSelecter(String curhref,Document doc , List<crawlerSelector> sels,boolean isTEXT){
		Object jqObj = doc;
		Elements array = null;
		Elements tempArray = null;
		String selectStr;
		boolean subMode = false;
		boolean netMode = false;
		for(crawlerSelector sel : sels) {
			tempArray = new Elements();
			selectStr = sel.Selector();
			
			if( selectStr.startsWith("-") ) {//减号开头的字符串
				subMode = true;
				selectStr = StringHelper.fixLeft(selectStr, "-");
			}
			
			if( selectStr.startsWith("^") ) {//^开头的字符串
				netMode = true;
				selectStr = StringHelper.fixLeft(selectStr, "^");
			}
			
			if( jqObj instanceof Elements ) {
				array = ((Elements)jqObj).select(selectStr);
			}
			if( jqObj instanceof Element ) {
				array = ((Element)jqObj).select(selectStr);
			}
			int maxi = array.size() ;
			if( maxi > 0 ) {
				int curPos = sel.CurrentIndex();
				if( sel.hasDirection() ) {
					int stepLength = sel.getStepLength();
					curPos = sel.getDirection() ? curPos - stepLength : curPos + stepLength;
				}

				for(int i = curPos; i< curPos + sel.Length(); i++ ) {
					if( i >= maxi ) {
						break;
					}
					tempArray.add( array.get(i) );
				}
			}
			if( subMode ) {//删除目标模式
				tempArray.remove();
			}
			else {//目标选择模式
				jqObj = tempArray;
			}
			if( tempArray.size() == 0  ) {
				break;
			}
			if( netMode ) {//网络目标模式
				Element ele = tempArray.get(0);//获得第一个元素
				if( ele.hasAttr("href") ) {
					String url = urlContent.filterURL(curhref, ele.attr("href")  );
					curhref = url;
					currentURL = curhref;
					try {
						jqObj =Jsoup.parse(request.page(curhref)) ;
					} catch (Exception e) {
						jqObj = null;
						break;
					}
				}
			}
		}
		
		Elements els =(Elements)jqObj;

		Element node = els != null && els.size() > 0 ? els.get(0) : null;
		String rString = null;
		Object ro = null;
		if( node != null ) {
			rString = (isTEXT ? node.text() : safeHTML(node));
			ro = (new JSONObject("url",currentURL)).puts("content", rString);
		}
		return ro;
	}
	
	/**解析生成选择器对象
	 * @param selecterString
	 * @return
	 */
	private List<crawlerSelector> getSelecter(String selecterString) {
		int startIdx;
		int length;
		int stepLength;
		List<crawlerSelector> rl = new ArrayList<>();
		Boolean directState = null;//null:未启用,true:向前,false:向后
		String[] grape0 = selecterString.split("\\|");
		int i = 0;
		for( ; i < grape0.length; i++) {
			startIdx = 0;
			length = 1;
			stepLength = 1;
			String[] groupA = grape0[i].split("&");//
			if( groupA.length > 1 ) {//包含选择器额外参数
				String tmpStr = groupA[1];
				if( groupA[1].indexOf("-") > 0 ) {//判断是否包含范围    
					String[] areaPair = groupA[1].split("-");
					startIdx = numberHelper.number2int(areaPair[0]);
					length = numberHelper.number2int(areaPair[1]);
				}
				if( groupA[1].indexOf("@") > 0 ) {//判断是否包含范围    
					String[] areaPair = groupA[1].split("@");
					tmpStr = areaPair[0];
					stepLength = numberHelper.number2int(areaPair[1]);
				}
				switch (tmpStr.toUpperCase()) {
					case "NEXT":
						directState = false;
						break;
					case "PREV":
						directState = true;
						break;
					case "FRIST":
						directState = null;
						startIdx = 0;
						break;
					case "END":
						directState = null;
						startIdx = -1;
						break;
					default:
						directState = null;
						startIdx = numberHelper.number2int(tmpStr.trim());
						break;
				}
			}
			rl.add(new crawlerSelector(groupA[0], startIdx, length, directState,stepLength));
		}
		return rl;
	}
	
	/**获得内容页URL
	 * @param host
	 * @param baseURL
	 * @param selecters
	 * @return
	 */
	private urlContent getURL(String host,String baseURL, String selecters){
		String[] sels = selecters.split(",");
		String url = host + baseURL;
		String tempURL = baseURL.startsWith("http:") ? baseURL : url;
		urlContent result = new urlContent(url, tempURL, tempURL);
		if( selecters == null || selecters.equals("") ) {
			return result;
		}
		
		try {
			Document doc = null;
			Elements array = null;
			int l = sels.length;
			doc = Jsoup.parse( request.page(tempURL) );
			Object jqObj = doc;
			for(int i =0; i < l; i++ ) {
				List<crawlerSelector> csl = getSelecter(sels[i]); 
				for(crawlerSelector sel : csl) {
					if( jqObj instanceof Document ) {
						array = ((Document)jqObj).select(sel.Selector());
					}
					if( jqObj instanceof Elements ) {
						array = ((Elements)jqObj).select(sel.Selector());
					}
					if( jqObj instanceof Element ) {
						array = ((Element)jqObj).select(sel.Selector());
					}
					int curPos = sel.CurrentIndex();
					if( sel.hasDirection() ) {//包含游标指针
						int stepLength = sel.getStepLength();
						curPos = sel.getDirection() ? curPos - stepLength : curPos + stepLength;
					}
					if( curPos == -1 ) {//是最后一个
						curPos = array.size() - 1;
					}
					jqObj = (array != null && array.size() > 0) ? array.get(curPos) : jqObj;//获得目标元素 
				}
				Element element = (Element)jqObj;//最后获得的肯定是Element
				result.setUp(baseURL);
				String nextURL = "";
				if( element.hasAttr("href") ) {//是否包含超链接
					nextURL = element.attr("href");
					if( StringHelper.InvaildString( nextURL ) ){
						tempURL = urlContent.filterURL(tempURL, nextURL);
						result.setCur(nextURL);
					}
					else {
						result = null;
						nlogger.login("输入的选择器href属性值不合法！ 选择器:" + tempURL + "->" + selecters);
					}
				}
				else {
					result = null;
					nlogger.login("输入的选择器不包含href属性！ 选择器:" + tempURL + "->" + selecters);
				}
			}

			
			//根据选择器从当前Doc找到新URL
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	/*
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		stateRun = false;
		super.finalize();
	}
	*/
	private GrapeTreeDBModel _db() {
		GrapeTreeDBModel db = new GrapeTreeDBModel();
		GrapeDBSpecField gdb = new GrapeDBSpecField();
		gdb.importDescription( appsProxy.tableConfig("crawlerTask") );
		db.descriptionModel(gdb).bind();
		return db;
	}
	public task() {
		db = _db();
		pkString = db.getPk();
	}
	@SuppressWarnings("unchecked")
	@apiType(tpye.sessionApi)
	public String insert(String json) {
		Object ob = null;
		JSONObject jsonObj = JSONObject.toJSON( codec.DecodeFastJSON( json ));
		if( jsonObj != null ){
			jsonObj.put("time", TimeHelper.nowMillis() );
			jsonObj.put("neartime", 0);
			jsonObj.put("runtime", 0);
			ob = db.data( jsonObj ).autoComplete().insertOnce();
			
		}
		return rMsg.netMSG(ob != null, (String)ob);
	}
	/**删除爬虫任务
	 * @param ids
	 * @return
	 */
	@apiType(tpye.sessionApi)
	public String delete(String ids) {
		long rl = 0;
		boolean rb = true;
		String[] id = ids.split(",");
		int l = id.length;
		if( l > 0 ){
			for( int i =0; i < l; i++ ){
				if( id[i].length() > 0 ){
					db.or().eq(pkString, id[i]);
				}
			}
			rl = rb ? db.deleteAll() : -1;
		}
		else{
			rb = false;
		}
		return rMsg.netMSG(rb, rl);
	}
	@apiType(tpye.sessionApi)
	public String updateOne(String eid,String json){
		if( !StringHelper.InvaildString(eid) ){
			return rMsg.netMSG(false, "无效任务");
		}
		JSONObject rjson = JSONObject.toJSON(codec.DecodeFastJSON(json));
		if( rjson == null ){
			return rMsg.netMSG(false, "非法操作");
		}
		JSONObject rJson = db.data(rjson).eq(pkString, eid).update();
		return rMsg.netState(rJson != null);
	}
	
	/**获得任务信息
	 * @param eid
	 * @return
	 */
	public String get(String eid){
		if( !StringHelper.InvaildString(eid) ){
			return rMsg.netMSG(false, "无效任务");
		}
		return rMsg.netMSG(true, db.eq(pkString, eid).find()); 
	}
	
	/**获得全部任务信息
	 * @return
	 */
	public String getAll(){
		return rMsg.netMSG(true, db.desc("time").select()); 
	}
	
	/**分页方式获得任务信息
	 * @param idx
	 * @param max
	 * @return
	 */
	public String page(int idx,int max){
		if( idx <= 0 ){
			return rMsg.netMSG(false, "页码错误");
		}
		if( max <= 0 ){
			return rMsg.netMSG(false, "页长度错误");
		}
		return rMsg.netPAGE(idx, max, db.dirty().count(), db.desc("time").page(idx, max));
	}
	
	/**根据条件获得分页数据
	 * @param idx
	 * @param max
	 * @param cond
	 * @return
	 */
	public String pageby(int idx,int max,String cond){
		String out = null;
		if( idx <= 0 ){
			return rMsg.netMSG(false, "页码错误");
		}
		if( max <= 0 ){
			return rMsg.netMSG(false, "页长度错误");
		}
		JSONArray condObj = org.json.simple.JSONArray.toJSONArray(cond);
		if( condObj != null ) {
			db.where(condObj).desc("time");
			out = rMsg.netPAGE(idx, max, db.dirty().count(), db.page(idx, max));
		}
		else {
			out = rMsg.netMSG(false, "无效条件");
		}
		return out;
	}
	//-----------------------------------------上面 是基础代码
	/*
	private boolean runState(String eid, int newState) {
		if( !StringHelper.InvaildString(eid) ){
			return false;
		}
		JSONObject obj = new JSONObject("runstate",newState);
		obj = db.eq(pkString, eid).data(obj).update();
		return obj != null;
	}
	*/
	
	private boolean taskState(String eid,int newState) {
		if( !StringHelper.InvaildString(eid) ){
			return false;
		}
		JSONObject obj = new JSONObject("state",newState);
		obj = db.eq(pkString, eid).data(obj).update();
		return obj != null;
	}
	
	/**任务启用
	 * @param eid
	 * @return
	 */
	public String enable(String eid) {
		if( !StringHelper.InvaildString(eid) ){
			return rMsg.netMSG(false, "非法数据");
		}
		return rMsg.netState( taskState(eid,1) );
	}
	/**任务关闭
	 * @param eid
	 * @return
	 */
	public String disable(String eid) {
		if( !StringHelper.InvaildString(eid) ){
			return rMsg.netMSG(false, "非法数据");
		}
		return rMsg.netState( taskState(eid,0) );
	}
	
	public String test(String str) {
		return "caption:" +  str;
	}
}
