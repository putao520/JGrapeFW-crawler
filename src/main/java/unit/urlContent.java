package unit;

import java.net.MalformedURLException;
import java.net.URL;

import com.sun.star.beans.StringPair;

import interfaceApplication.task.state;
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
	public final static String url2string(URL currentURL) {
		return currentURL.getProtocol() + "://" + currentURL.getHost() + (currentURL.getPort() > 0 ? ":" + currentURL.getPort() : "");
	}
	/**根据当前浏览器URL和目标URL转换成正确的URL
	 * @param curhref
	 * @param url
	 * @return
	 */
	/*
	public final static String filterURL(String curhref,String url) {
		if( curhref != null && !url.toLowerCase().startsWith("http") ) {
			String[] array = url.split("/");
			curhref += "xxx";
			URL currentURL;
			try {
				currentURL = new URL(curhref);
				String _paths = currentURL.getPath();
				if( array[0].equals(".") ) {// ./asd.html的结构， host后紧跟URL
					String[] paths = _paths.split("/");
					String newpath = StringHelper.join( paths , "/", 0 , paths.length - 1);

					url = StringHelper.fixString(url2string( currentURL ) + newpath,"/") + StringHelper.fixLeft(url, ".");
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
					String[] paths = StringHelper.fixLeft(_paths, "/").split("/");
					
					if( i <= paths.length ) {
						String newpath = StringHelper.join(paths, "/", 0, paths.length - i );
						if( newpath.length() > 0 ) {
							newpath += "/";
						}
						url = url2string( currentURL ) + "/" + newpath + StringHelper.join(array, "/", i-1,-1);
					}
					else {
						nlogger.logout("curhref:" + curhref + " url:" + url + "向上层级异常");
					}
				}
				else {
					if( !url.startsWith("/") ) {
						url = "/" + url;
					}
					url = url2string( currentURL ) + url;
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return url;
	}
	*/
	public urlContent setCur(String url) {
		curURL = url;
		return this;
	}
	public urlContent setUp(String url) {
		uplevelURL = url;
		return this;
	}
}