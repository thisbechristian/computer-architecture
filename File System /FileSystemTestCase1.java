public class FileSystemTestCase1{

	public static void main(String[] args) {
	
		FileSystem F = new FileSystem(-1);
		
		System.out.println("dir \\");
		F.dir("\\");
		
		
		System.out.println("externalcopy pitt.png \\");
		F.externalcopy("pitt.png","\\");
		
		
		System.out.println("copy \\pitt.png \\yyy");
		F.copy("pitt.png","\\yyy");
		
		
		System.out.println("mkdir \\zzz");
		F.mkdir("\\zzz");
		
		System.out.println("copy \\yyy \\zzz");
		F.copy("\\yyy","\\zzz");
		
		System.out.println("dir \\zzz");
		F.dir("\\zzz");
		
		System.out.println("del \\pitt.png");
		F.del("\\pitt.png");

		
		System.out.println("copy \\zzz\\yyy \\aaa");
		F.copy("\\zzz\\yyy","\\aaa");
		
		System.out.println("dir \\");
		F.dir("\\");

    }

}