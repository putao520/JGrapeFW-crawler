package interfaceApplication;

public class crawlerSelector {
	private String selectString;
	private int startIdx = 0;
	private int len = 0;
	private int directStep = 0;
	private Boolean direction = null;//null,不启用，true向前，false向后
	public crawlerSelector(String sel) {
		init(sel,0,1,true,1);
	}
	public crawlerSelector(String sel,int start) {
		init(sel,start,1,true,1);
	}
	public crawlerSelector(String sel,int start,int length,Boolean direct,int directStep) {
		init(sel,start,length,direct,directStep);
	}
	private void init(String sel,int start,int length,Boolean direct,int directStep) {
		this.selectString = sel;
		this.startIdx = start;
		this.len = length;
		this.directStep = directStep;
		this.direction = direct;
	}
	/**选择器
	 * @return
	 */
	public String Selector() {
		return selectString;
	}
	/**起始索引
	 * @return
	 */
	public int CurrentIndex() {
		return startIdx;
	}
	/**操作长度
	 * @return
	 */
	public int Length() {
		return len;
	}
	/**是否使用方向
	 * @return
	 */
	public boolean hasDirection() {
		return direction != null;
	}
	/**获得方向
	 * @return
	 */
	public boolean getDirection() {
		return (direction != null) ? direction.booleanValue() : false;
	}
	/**获得方向
	 * @return
	 */
	public int getStepLength() {
		return (direction != null) ? directStep : 0;
	}
}
