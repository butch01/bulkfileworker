package bulkFileWorker;

public class FileCopy 
{

	private String source=null;
	private String target=null;

	public FileCopy(String source, String target)
	{
		this.target = target;
		this.source = source;
	}
	
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
}
