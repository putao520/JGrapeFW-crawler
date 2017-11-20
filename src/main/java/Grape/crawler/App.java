package Grape.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sun.star.configuration.theDefaultProvider;

import apps.appIns;
import apps.appsProxy;
import httpClient.request;
import interfaceApplication.task;
import java_cup.runtime.lr_parser;
import java_cup.runtime.virtual_parse_stack;
import security.codec;
import time.TimeHelper;
import unit.urlContent;

/**
 * Hello world!
 *
 */
public class App 
{
    @SuppressWarnings("unchecked")
	public static void main( String[] args )
    {	    	
    	//System.out.println(request.Get("http://www.ahgzw.gov.cn/gzwweb/list.jsp?strWebSiteId=1448865560847002&strColId=1448867943579006&_index=1"));
    	
    	//System.out.println( urlContent.filterURL("http://www.baidu.com/a/b/", "index.html") );
	    //System.out.println( urlContent.filterURL("http://syj.tl.gov.cn/2205/2212/sjcjxx", "./201710/t20171025_398986.html") );

//        System.out.println( "Hello World!" );
//        JSONObject json = new JSONObject();
//        json.put("configName","{\"cache\":\"redis\",\"other\":[],\"db\":\"mongodb\"}");
//        json.put("tableConfig", "{\"crawlerTask\":{\"tableName\":\"crawlerTask\",\"rule\":[{\"fieldName\":\"name\",\"fieldType\":0,\"initValue\":\"\",\"failedValue\":\"\",\"checkType\":1},{\"fieldName\":\"desc\",\"fieldType\":0,\"initValue\":\"\",\"failedValue\":\"\",\"checkType\":1},{\"fieldName\":\"info\",\"fieldType\":0,\"initValue\":\"\",\"failedValue\":\"\",\"checkType\":1},{\"fieldName\":\"state\",\"fieldType\":0,\"initValue\":0,\"failedValue\":0,\"checkType\":6},{\"fieldName\":\"runtime\",\"fieldType\":0,\"initValue\":\"\",\"failedValue\":\"\",\"checkType\":27},{\"fieldName\":\"neartime\",\"fieldType\":0,\"initValue\":\"\",\"failedValue\":\"\",\"checkType\":1},{\"fieldName\":\"runstate\",\"fieldType\":0,\"initValue\":0,\"failedValue\":0,\"checkType\":6},{\"fieldName\":\"time\",\"fieldType\":0,\"initValue\":0,\"failedValue\":0,\"checkType\":27}]}}");
//        appsProxy.testConfigValue(17, "crawler", json );
//        
//        //appsProxy.proxyCall("/crawler/task/DelayBlock");
//        task _task = new task();
//        //_task.startService();//为当前APP开启定时服务
//        _task.DelayBlock();
        
        /*
        //System.out.println( _task.test("putao520") );
        json = new JSONObject();
        
        json.put("name", "测试任务");
        json.put("desc", "测试任务的说明");
        json.put("time", TimeHelper.nowMillis());
        json.put("state", 0);
        json.put("runtime", 60 * 1000);
        json.put("neartime", TimeHelper.nowMillis());
        json.put("runstate", 0);
        json.put("owner", "putao520");
        JSONObject _info = new JSONObject();
        _info.put("host", "http://www.putao282.com");
        _info.put("init", (new JSONObject("base","/base.php")).puts("selecter", ""));
        _info.put("loop", (new JSONObject("mode",1)).puts("selecter", ""));
        JSONArray _array = new JSONArray();
        _array.add((new JSONObject("key","title")).puts("selecter", "").puts("isTEXT", true));
        _info.put("data", _array);
        _info.put("collectApi", "http://api.putao282.com");
        json.put("info", _info);
        */
        /*
        String rs = _task.insert(json.toJSONString());//测试添加任务
        System.out.println(rs);
        JSONObject updateJSON = new JSONObject();
        updateJSON.puts("runstate", 1);
        rs = _task.updateOne("59eda67949d3e095344d5054", codec.EncodeHtmlTag(codec.encodebase64(updateJSON.toJSONString())) );//测试更新任务
        System.out.println(rs);
        System.out.println( _task.delete("59eda67949d3e095344d5054") );//测试删除任务
        */

        
    }
}

