public class FileSystemDriver{

	public static void main(String[] args) {
	
		FileSystem F = new FileSystem(5);
		
		F.mkdir("\\Level1");
		F.mkdir("\\Level1\\Level2");
		F.mkdir("\\Level1\\Level2\\Level3");
		
		F.dir("\\");
		F.dir("\\Level1");
		F.dir("\\Level1\\Level2");
		
		F.externalcopy("test.txt","Level1\\Level2");
		F.externalcopy("test.txt","\\");
		
		F.copy("\\Level1\\Level2\\test.txt","Level1");
		
		F.dir("\\");
		F.dir("\\Level1");
		F.dir("\\Level1\\Level2");
		
		F.del("\\test.txt");
		F.del("\\Level1\\test.txt");
		
		
		F.dir("\\");
		F.dir("\\Level1");
		F.dir("\\Level1\\Level2");
		

		F.mkdir("\\Level1.1");
		F.mkdir("\\Level1.2");
		F.mkdir("\\Level1.3");
		F.mkdir("\\Level1.4");
		
		
		F.dir("\\");
		F.dir("\\Level1");
		F.dir("\\Level1\\Level2");
		
    }


}