package unit;

import java.net.MalformedURLException;
import java.net.URL;

import nlogger.nlogger;
import string.StringHelper;

public class urlContent{
	private String curhref;
	private String curURL;
	private String uplevelURL;
	
	public urlContent(String href,String curURL,String upLevelURL) {
		curhref = href;
		this.curURL = curURL ;
		this.uplevelURL = upLevelURL;
	}
	public urlContent href(String href) {
		this.curhref = href;
		return this;
	}
	public String getCur() {
		return curURL;
	}
	public String getUp() {
		return uplevelURL;
	}
	/**根据当前浏览器URL和目标URL转换成正确的URL
	 * @param curhref
	 * @param url
	 * @return
	 */
	public final static String filterURL(String curhref,String url) {
		if( curhref != null && !url.toLowerCase().startsWith("http") ) {
			String[] array = url.split("/");
			URL currentURL;
			try {
				currentURL = new URL(curhref);
				if( array[0].equals(".") ) {// ./asd.html的结构， host后紧跟URL
					url = currentURL.getProtocol() + "://" + currentURL.getHost() + StringHelper.fixLeft(url, ".");
				}
				else if( array[0].equals("..") ) {// ../../../asd.html无限向上关系
					int i = 0;
					for(i=0; i<array.length; i++) {
						if( !array[i].equals("..") ) {
							break;
						}
					}
					i++;
					//i = 多少个..向上的层级
					String[] paths = StringHelper.fixLeft(currentURL.getPath(), "/").split("/");
					
					if( i <= paths.length ) {
						String newpath = StringHelper.join(paths, "/", 0, paths.length - i );
						url = currentURL.getProtocol() + "://" + currentURL.getHost() + "/" + (newpath.equals("") ? "" : "/") + StringHelper.join(array, "/", i-1,-1);
					}
					else {
						nlogger.logout("curhref:" + curhref + " url:" + url + "向上层级异常");
					}
				}
				else {
					url = currentURL.getProtocol() + "://" + currentURL.getHost() + url;
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/*
			if( StringHelper.left(url, 1).equals(".") ) {//如果是点开头
				curhref += " ";
				String[] urls = curhref.split("/");
				
				urls[ urls.length - 1] =StringHelper.fixLeft(StringHelper.fixLeft(url, "."), "/");
				url = StringHelper.join(urls, "/" ).trim();
			}
			*/
		}
		return url;
	}
	public urlContent setCur(String url) {
		curURL = filterURL(curhref,url);
		return this;
	}
	public urlContent setUp(String url) {
		uplevelURL = filterURL(curhref,url);
		return this;
	}
}