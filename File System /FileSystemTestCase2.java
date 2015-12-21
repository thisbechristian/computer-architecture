public class FileSystemTestCase2{

	public static void main(String[] args) {
	
		int raid = 5;
	
		FileSystem F = new FileSystem(raid);
		
		System.out.println("dir \\");
		F.dir("\\");
		
		System.out.println("externalcopy pitt.png \\");
		F.externalcopy("pitt.png","\\");
		
		System.out.println("dir \\");
		F.dir("\\");

    }

}