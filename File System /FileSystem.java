import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class FileSystem {
	
	private int MAXSECTOR = 32;
	private Sector rootOfFileSystem;
	public LinkedList<Integer> freeSectors;
	public static  StorageSystem fileStorageSystem;


	public FileSystem(int raid) {
		freeSectors = new LinkedList<Integer>();
		fileStorageSystem = new StorageSystem(raid);
		rootOfFileSystem = this.Read(0);
		
		
		if(rootOfFileSystem == null){
			for(int i = 1; i < MAXSECTOR; i++){
				freeSectors.add(i);
			}
			
			rootOfFileSystem = new Sector(0,freeSectors);
			this.Write(0,rootOfFileSystem);
			
		}
		
		else{
			freeSectors = rootOfFileSystem.freeSectors;
		}
	
	}
	
	private static byte[] serialize(Object obj) {
		try{
    		ByteArrayOutputStream out = new ByteArrayOutputStream();
    		ObjectOutputStream os = new ObjectOutputStream(out);
    		os.writeObject(obj);
    		os.flush(); 
      		os.close(); 
      		out.close();
      		byte [] data = out.toByteArray();
      		return data;
    	}
    	catch(Exception E){return null;}
	}
	
	private static Sector deserialize(byte[] data) {
    	try{
    		ByteArrayInputStream in = new ByteArrayInputStream(data);
    		ObjectInputStream is = new ObjectInputStream(in);
    		return (Sector)is.readObject();
    	}
    	catch(Exception Ex){return null;}
	}
	
	private void Write(int sectorNum, Sector theSector) {
		byte[] data = this.serialize(theSector);
		
		if(data.length < 8000){
      		data = extendByteArray(data);
      	}
		
		fileStorageSystem.Write(sectorNum,data);
	} 
	
	private Sector Read(int sectorNum){
		Sector theSector = (Sector) this.deserialize(fileStorageSystem.Read(sectorNum));
		return theSector;
	} 

	public void mkdir(String path) {
	
		String[] names = pathToArray(path);
		
		if(!names[0].equals("root")){
			mkdir_r(names, 0, rootOfFileSystem);
		}
	}
	
	private void mkdir_r(String[] names, int i, Sector currDirectory) {
		
		if(i >= names.length){
			return;
		}
		
		Node tempNode = new Node(names[i]);
		
		if(i < names.length && !currDirectory.children.contains(tempNode)){
			int newSectorNum = freeSectors.remove();
			this.Write(0,rootOfFileSystem);
			Node newDirectory = new Node(names[i]);
			newDirectory.sectors.add(newSectorNum);
			currDirectory.children.add(newDirectory);
			this.Write(currDirectory.sectorNumber,currDirectory);
			Sector newSector = new Sector(newSectorNum);
			this.Write(newSectorNum,newSector);
			i++;
			mkdir_r(names, i, newSector);
		}
		
		else if(i < (names.length-1)){
			Node nextDirectory =  currDirectory.children.get(currDirectory.children.indexOf(tempNode));
			currDirectory = this.Read(nextDirectory.sectors.get(0));
			i++;
			mkdir_r(names, i, currDirectory);
		}
		
		else if(i == (names.length-1)){
			System.out.println("Directory name already exists");
			return;
		}
		
		return;
		
	}
	
	public void rmdir(String path) {
	
		String[] names = pathToArray(path);
		Sector deleteDirectory;
		Sector parentDirectory;
		
		if(names.length > 1){
			String[] tempNames = new String[names.length-1];
			
			for(int i = 0; i < (names.length-1); i++){
				tempNames[i] = names[i];
			}
			deleteDirectory = getCurrentDirectory(names, 0, rootOfFileSystem);
			parentDirectory = getCurrentDirectory(tempNames, 0, rootOfFileSystem);
		
		}
		
		else{
			deleteDirectory = getCurrentDirectory(names, 0, rootOfFileSystem);
			parentDirectory = rootOfFileSystem;
		}
		
		if(deleteDirectory != null && !names[0].equals("root")){
		
			if (!(deleteDirectory.children.size() > 0)){
				Node tempNode = new Node(names[names.length-1]);
				
				freeSectors.add(0,deleteDirectory.sectorNumber);
				this.Write(0,rootOfFileSystem);
				
				parentDirectory.children.remove(tempNode);
				this.Write(parentDirectory.sectorNumber,parentDirectory);
			}
			
			else{
				System.out.println("Directory given is not empty cannot delete");
			}
			
		}
		
		else if(names[0].equals("root")){
			System.out.println("Cannot delete Root Directory");
		}
		
		else{
			System.out.println("Directory does not exist");
		}
	}
	
	public void dir(String path) {
	
		Sector currDirectory;
		
		String[] names = pathToArray(path);
	
		if(!names[0].equals("root")){
			currDirectory = getCurrentDirectory(names, 0, rootOfFileSystem);
		}
		
		else{
			currDirectory = rootOfFileSystem;
		}
		
		if(currDirectory != null){
		
		Node[] listOfContents = new Node[currDirectory.children.size()];
		listOfContents = currDirectory.children.toArray(listOfContents);
		
		if(listOfContents.length > 0){
			for(Node n : listOfContents){
					System.out.println(n.name);
				}
			}
		
			else{
				System.out.println("Directory is empty");
			}
		
		}
		
		else{
			System.out.println("Directory does not exist");
		}
	}
	
	public void del(String path) {
	
		String[] names = pathToArray(path);
		Sector parentDirectory;
		
		if(names.length > 1){
			String[] tempNames = new String[names.length-1];
			
			for(int i = 0; i < (names.length-1); i++){
				tempNames[i] = names[i];
			}
			parentDirectory = getCurrentDirectory(tempNames, 0, rootOfFileSystem);
		
		}
		
		else{
			parentDirectory = rootOfFileSystem;
		}
		
		Node tempNode = new Node(names[names.length-1]);
		if(parentDirectory != null && parentDirectory.children.contains(tempNode)){
		
			Node deleteFile = parentDirectory.children.get(parentDirectory.children.indexOf(tempNode));
			
			for(int i = 0; i < deleteFile.sectors.size(); i++){
				freeSectors.add(0,deleteFile.sectors.get(i));
				this.Write(0,rootOfFileSystem);
			}
				
			parentDirectory.children.remove(deleteFile);
			this.Write(parentDirectory.sectorNumber,parentDirectory);
		}
		
		else{
			System.out.println("Filename or Path does not exist");
		}
	}
	
	public void externalcopy(String filename, String path){
	
		String[] names = pathToArray(path);
		Sector currDirectory;
		
		if(names[0].equals("root")){
			currDirectory = rootOfFileSystem;
		}
		
		else{
			currDirectory = getCurrentDirectory(names, 0, rootOfFileSystem);
		}
		
		if(currDirectory != null){
	
		try{
			Path filepath = Paths.get(filename);
			byte[] data = Files.readAllBytes(filepath);
			
			Node tempNode = new Node(filename);
			
			if(!currDirectory.children.contains(tempNode)){
		
				int newSectorNum;
				Node newFile = new Node(filename);			
			
				if(data.length <= 8000){
					newSectorNum = freeSectors.remove();
					this.Write(0,rootOfFileSystem);
					newFile.sectors.add(newSectorNum);
					data = extendByteArray(data);
					fileStorageSystem.Write(newSectorNum,data);
				}
				
				else{
				
					byte[] tempData = new byte[8000];
					
					for(int i = 0; i < data.length; i++){
						
						tempData[i % 8000] = data[i];
						
						if( (i % 8000 == 0) && (i != 0) ){
							newSectorNum = freeSectors.remove();
							this.Write(0,rootOfFileSystem);
							newFile.sectors.add(newSectorNum);
							fileStorageSystem.Write(newSectorNum,tempData);
							tempData = new byte[8000];
						}	
					}
					
					newSectorNum = freeSectors.remove();
					this.Write(0,rootOfFileSystem);
					newFile.sectors.add(newSectorNum);
					fileStorageSystem.Write(newSectorNum,tempData);	
				}
				
				currDirectory.children.add(newFile);
				this.Write(currDirectory.sectorNumber,currDirectory);
			}
			
		
			else{
				System.out.println("File name already exists");
			}
		}
		
		catch(Exception ex){System.out.println("External file path does not exist");}	
		
		}
		
		else{
			mkdir(path);
			externalcopy(filename, path);
		}

	}
	
	public void copy(String sourcePath, String destPath) {
	
		String[] names = pathToArray(destPath);
		String[] names1 = pathToArray(sourcePath);
		Sector sourceDirectory;
		Sector destinationDirectory;
		
		if(names1.length > 1){
			String[] tempNames = new String[names1.length-1];
			
			for(int i = 0; i < (names1.length-1); i++){
				tempNames[i] = names1[i];
			}
			sourceDirectory = getCurrentDirectory(tempNames, 0, rootOfFileSystem);
		}
		
		else{
			sourceDirectory = rootOfFileSystem;
		}
		
		if(names[0].equals("root")){
			destinationDirectory = rootOfFileSystem;
		}
		
		else{
			destinationDirectory = getCurrentDirectory(names, 0, rootOfFileSystem);
		}
		
		if(sourceDirectory != null && destinationDirectory != null && sourcePath.contains(".")){
			
			StringBuilder copyname = new StringBuilder();
			copyname.append(names1[names1.length-1]);
		
			Node tempNode = new Node(names1[names1.length-1]);
			Node copyFile = sourceDirectory.children.get(sourceDirectory.children.indexOf(tempNode));
			Node newFile = new Node(copyname.toString());
			
			if(copyFile != null && !destinationDirectory.children.contains(newFile)){
			
				int oldSectorNum;
				int newSectorNum;
			
				for(int i = 0; i < copyFile.sectors.size(); i++){
					oldSectorNum = copyFile.sectors.get(i);
					newSectorNum = freeSectors.remove();
					this.Write(0,rootOfFileSystem);
					fileStorageSystem.Write(newSectorNum,fileStorageSystem.Read(oldSectorNum));
					newFile.sectors.add(newSectorNum);
				}
			
				destinationDirectory.children.add(newFile);
				this.Write(destinationDirectory.sectorNumber,destinationDirectory);
			}
			
			else if(copyFile == null){
				System.out.println("Filename does not exist");
			}
			
			else{
				System.out.println("Filename already exists in destination directory");
			}
				
		}
		
		else if(sourceDirectory != null && destinationDirectory != null && !sourcePath.contains(".")){
			
			sourceDirectory = getCurrentDirectory(names1, 0, rootOfFileSystem);
			
			StringBuilder newDestPath = new StringBuilder(destPath);
			newDestPath.append("\\");
			newDestPath.append(names1[names1.length-1]);
			mkdir(newDestPath.toString());
			
			for(int i = 0; i < sourceDirectory.children.size(); i++){
				StringBuilder newSourcePath = new StringBuilder(sourcePath);
				newSourcePath.append("\\");
				newSourcePath.append(sourceDirectory.children.get(i).name);
				copy(newSourcePath.toString(),newDestPath.toString());
			}
			
		}
		
		else if(destinationDirectory == null){
			mkdir(destPath);
			copy(sourcePath, destPath);
		}

		else{
			System.out.println("Filename path does not exist");
		}
	
	}
	
	private Sector getCurrentDirectory(String[] names, int i, Sector currDirectory) {
		
		if(i >= names.length){
			return null;
		}
		
		Node tempNode = new Node(names[i]);
		
		if(!currDirectory.children.contains(tempNode)){
			return null;
		}
	
		else if(i == (names.length-1)){
			Node foundDirectory = currDirectory.children.get(currDirectory.children.indexOf(tempNode));
			currDirectory = this.Read(foundDirectory.sectors.get(0));
			return currDirectory;
		}
	
		else if(i < (names.length-1)){
			Node nextDirectory =  currDirectory.children.get(currDirectory.children.indexOf(tempNode));
			currDirectory = this.Read(nextDirectory.sectors.get(0));
			i++;
			return getCurrentDirectory(names, i, currDirectory);
		}
		
		else{
			return null;
		}
		
	}
	
	private String[] pathToArray(String path){
	
		String[] names;
		
		if(path.equals("\\")){
			names = new String[1];
			names[0] = "root";
		}
		
		else{
		
			StringBuilder modifyPath = new StringBuilder(path);
		
			if(modifyPath.charAt(0) == '\\'){
				modifyPath.deleteCharAt(0);
				path = modifyPath.toString();
			}
	
			names = path.split("\\\\");	
		}
		
		return names;
	
	}
	
	private static byte[] extendByteArray(byte[] data){
	
		byte [] extendBytes = new byte[8000];
		
      	for(int i = 0; i < data.length; i++){
      		extendBytes[i] = data[i];
      	}
      	
      	for(int i = data.length; i < 8000; i++){
      		extendBytes[i] = 0;
      	}
      			
      	return extendBytes;
	
	}
	
	
}



class Sector implements Serializable {

	public int sectorNumber;
	public ArrayList<Node> children;
	public LinkedList<Integer> freeSectors;
	
	public Sector(int n){
		sectorNumber = n;
		children = new ArrayList<Node>();
	}
	
	public Sector(int n, LinkedList<Integer> list){
		sectorNumber = n;
		children = new ArrayList<Node>();
		freeSectors = list;
	}

}


class Node implements Serializable {
	public String name;
	public ArrayList<Integer> sectors;
	
	public Node(String n){
		name = new String(n.toLowerCase());
		sectors = new ArrayList<Integer>();
	}
	
	public boolean equals(Object obj) {
		Node n = (Node) obj;
		return(name.equals(n.name.toLowerCase()));
	}
}



class StorageSystem {
	
	int raid;
	
	public StorageSystem(int r) {
		raid = r;
	}
	
	
	public void Write(int sector, byte[] data) {

		try {
		
			if(raid != 0 && raid != 4 && raid != 5){
				
				StringBuilder filename = new StringBuilder("Disk/Sector");
				filename.append(sector);
				filename.append(".dat");
			
				FileOutputStream fout = new FileOutputStream(filename.toString());
				fout.write(data);
				fout.close();
			}
			
			if(raid == 1){
				StringBuilder filename = new StringBuilder("Disk_Mirror/Sector");
				filename.append(sector);
				filename.append(".dat");
			
				FileOutputStream fout = new FileOutputStream(filename.toString());
				fout.write(data);
				fout.close();
			}
			
			if(raid == 0){
				
				StringBuilder filename;
				
				if((sector % 3) == 0){
					filename = new StringBuilder("Disk1/Sector");
				}
				
				else if((sector % 3) == 1){
					filename = new StringBuilder("Disk2/Sector");
				}
				
				else{
					filename = new StringBuilder("Disk3/Sector");
				}
			
				filename.append(sector);
				filename.append(".dat");
				FileOutputStream fout = new FileOutputStream(filename.toString());
				fout.write(data);
				fout.close();
			}
			
			if(raid == 4){
				
				StringBuilder filename1 = new StringBuilder();
				StringBuilder filename2 = new StringBuilder();
				
				int paritySector = 0;
				boolean parityNameFlag = false;
				
				if((sector % 2) == 0){
					paritySector = sector+1;
					filename1 = new StringBuilder("Disk1/Sector");
					filename2 = new StringBuilder("Disk2/Sector");
					parityNameFlag = true;
				}
				
				else if((sector % 2) == 1){
					paritySector = sector-1;
					filename1 = new StringBuilder("Disk2/Sector");
					filename2 = new StringBuilder("Disk1/Sector");
				}
				
				filename1.append(sector);
				filename1.append(".dat");
				filename2.append(paritySector);
				filename2.append(".dat");
				
				StringBuilder parityfile;
				parityfile = new StringBuilder("Disk3/Sector");
				
				if(parityNameFlag){
					parityfile.append(sector);
					parityfile.append(paritySector);
					parityfile.append(".dat");
				}
				
				else{
					parityfile.append(paritySector);
					parityfile.append(sector);
					parityfile.append(".dat");
				}
				
				
				Path path;
				File file = new File(filename2.toString());
				byte[] data2;
				byte[] parityData;
				
				if(file.exists() && !file.isDirectory()){
					path = Paths.get(filename2.toString());
					data2 = Files.readAllBytes(path);
					parityData = xor(data,data2);
				}
				
				else{
					parityData = data;
				}
				
				FileOutputStream fout1 = new FileOutputStream(filename1.toString());
				FileOutputStream fout2 = new FileOutputStream(parityfile.toString());
				fout1.write(data);
				fout2.write(parityData);
				fout1.close();
				fout2.close();
			}
			
			if(raid == 5){
				
				StringBuilder filename1 = new StringBuilder();
				StringBuilder filename2 = new StringBuilder();
				StringBuilder parityfile = new StringBuilder();
				
				int paritySector = 0;
				boolean parityNameFlag = false;
				
				if((sector/2) % 3 == 0){
				
					parityfile = new StringBuilder("Disk3/Sector");
					
					if(sector % 2 == 0){
						paritySector = sector+1;
						filename1 = new StringBuilder("Disk1/Sector");
						filename2 = new StringBuilder("Disk2/Sector");
						parityNameFlag = true;
					}
					
					else{
						paritySector = sector-1;
						filename1 = new StringBuilder("Disk2/Sector");
						filename2 = new StringBuilder("Disk1/Sector");
					}
				}
				
				else if((sector/2) % 3 == 1){
				
					parityfile = new StringBuilder("Disk2/Sector");
					
					if(sector % 2 == 0){
						paritySector = sector+1;
						filename1 = new StringBuilder("Disk3/Sector");
						filename2 = new StringBuilder("Disk1/Sector");
						parityNameFlag = true;
					}
					
					else{
						paritySector = sector-1;
						filename1 = new StringBuilder("Disk1/Sector");
						filename2 = new StringBuilder("Disk3/Sector");
					}
				}
				
				else{
				
					parityfile = new StringBuilder("Disk1/Sector");
					
					if(sector % 2 == 0){
						paritySector = sector+1;
						filename1 = new StringBuilder("Disk2/Sector");
						filename2 = new StringBuilder("Disk3/Sector");
						parityNameFlag = true;
					}
					
					else{
						paritySector = sector-1;
						filename1 = new StringBuilder("Disk3/Sector");
						filename2 = new StringBuilder("Disk2/Sector");
					}
				}
				
				filename1.append(sector);
				filename1.append(".dat");
				filename2.append(paritySector);
				filename2.append(".dat");
				
				
				if(parityNameFlag){
					parityfile.append(sector);
					parityfile.append(paritySector);
					parityfile.append(".dat");
				}
				
				else{
					parityfile.append(paritySector);
					parityfile.append(sector);
					parityfile.append(".dat");
				}
				
				
				Path path;
				File file = new File(filename2.toString());
				byte[] data2;
				byte[] parityData;
				
				if(file.exists() && !file.isDirectory()){
					path = Paths.get(filename2.toString());
					data2 = Files.readAllBytes(path);
					parityData = xor(data,data2);
				}
				
				else{
					parityData = data;
				}
				
				FileOutputStream fout1 = new FileOutputStream(filename1.toString());
				FileOutputStream fout2 = new FileOutputStream(parityfile.toString());
				fout1.write(data);
				fout2.write(parityData);
				fout1.close();
				fout2.close();
			}
			
	   	}
	   	
	   	catch(Exception ex){}
	}
	
	public byte[] Read(int sector) {
	
		byte[] data = null;
		
		try{
			if(raid != 0 && raid != 4 && raid != 5){
			
				StringBuilder filename = new StringBuilder("Disk/Sector");
				filename.append(sector);
				filename.append(".dat");
				Path path;
				
				File file = new File(filename.toString());
				
				if(file.exists() && !file.isDirectory()){
					path = Paths.get(filename.toString());
					data = Files.readAllBytes(path);
				}

				else if(raid == 1){
					filename = new StringBuilder("Disk_Mirror/Sector");
					filename.append(sector);
					filename.append(".dat");
					
					file = new File(filename.toString());
				
					if(file.exists() && !file.isDirectory()){
						path = Paths.get(filename.toString());
						data = Files.readAllBytes(path);
					}
				}
				
		   	}
		   	
		   	else if(raid == 0){
		   	
		   		StringBuilder filename;
		   	
		   		if((sector % 3) == 0){
					filename = new StringBuilder("Disk1/Sector");
				}
				
				else if((sector % 3) == 1){
					filename = new StringBuilder("Disk2/Sector");
				}
				
				else{
					filename = new StringBuilder("Disk3/Sector");
				}
				
				filename.append(sector);
				filename.append(".dat");
				Path path;
				
				File file = new File(filename.toString());
				
				if(file.exists() && !file.isDirectory()){
					path = Paths.get(filename.toString());
					data = Files.readAllBytes(path);
				}
		   	}
		   	
		   	else if(raid == 4){
		   	
		   		StringBuilder filename1 = new StringBuilder();
				StringBuilder filename2 = new StringBuilder();
				
				int paritySector = 0;
				boolean parityNameFlag = false;
				
				if((sector % 2) == 0){
					paritySector = sector+1;
					filename1 = new StringBuilder("Disk1/Sector");
					filename2 = new StringBuilder("Disk2/Sector");
					parityNameFlag = true;
				}
				
				else if((sector % 2) == 1){
					paritySector = sector-1;
					filename1 = new StringBuilder("Disk2/Sector");
					filename2 = new StringBuilder("Disk1/Sector");
				}
				
				filename1.append(sector);
				filename1.append(".dat");
				filename2.append(paritySector);
				filename2.append(".dat");
				
				StringBuilder parityfile;
				parityfile = new StringBuilder("Disk3/Sector");
				
				if(parityNameFlag){
					parityfile.append(sector);
					parityfile.append(paritySector);
					parityfile.append(".dat");
				}
				
				else{
					parityfile.append(paritySector);
					parityfile.append(sector);
					parityfile.append(".dat");
				}
				
				Path path1;
				Path path2;
				Path path3;
				File file1 = new File(filename1.toString());
				File file2 = new File(filename2.toString());
				File file3 = new File(parityfile.toString());
				byte[] data2;
				byte[] parityData;
				
				if(file1.exists() && !file1.isDirectory()){
					path1 = Paths.get(filename1.toString());
					data = Files.readAllBytes(path1);
				}
				
				else if(file2.exists() && !file2.isDirectory() && file3.exists() && !file3.isDirectory()) {
					path2 = Paths.get(filename2.toString());
					path3 = Paths.get(parityfile.toString());
					data2 = Files.readAllBytes(path2);
					parityData = Files.readAllBytes(path3);
					data = xor(data2,parityData);
				}
				
				else if(file3.exists() && !file3.isDirectory()){
					path3 = Paths.get(parityfile.toString());
					data = Files.readAllBytes(path3);
				}
		   	}
		   	
		   	else if(raid == 5){
		   	
		   		StringBuilder filename1 = new StringBuilder();
				StringBuilder filename2 = new StringBuilder();
				StringBuilder parityfile = new StringBuilder();
				
				int paritySector = 0;
				boolean parityNameFlag = false;
				
				if((sector/2) % 3 == 0){
				
					parityfile = new StringBuilder("Disk3/Sector");
					
					if(sector % 2 == 0){
						paritySector = sector+1;
						filename1 = new StringBuilder("Disk1/Sector");
						filename2 = new StringBuilder("Disk2/Sector");
						parityNameFlag = true;
					}
					
					else{
						paritySector = sector-1;
						filename1 = new StringBuilder("Disk2/Sector");
						filename2 = new StringBuilder("Disk1/Sector");
					}
				}
				
				else if((sector/2) % 3 == 1){
				
					parityfile = new StringBuilder("Disk2/Sector");
					
					if(sector % 2 == 0){
						paritySector = sector+1;
						filename1 = new StringBuilder("Disk3/Sector");
						filename2 = new StringBuilder("Disk1/Sector");
						parityNameFlag = true;
					}
					
					else{
						paritySector = sector-1;
						filename1 = new StringBuilder("Disk1/Sector");
						filename2 = new StringBuilder("Disk3/Sector");
					}
				}
				
				else{
				
					parityfile = new StringBuilder("Disk1/Sector");
					
					if(sector % 2 == 0){
						paritySector = sector+1;
						filename1 = new StringBuilder("Disk2/Sector");
						filename2 = new StringBuilder("Disk3/Sector");
						parityNameFlag = true;
					}
					
					else{
						paritySector = sector-1;
						filename1 = new StringBuilder("Disk3/Sector");
						filename2 = new StringBuilder("Disk2/Sector");
					}
				}
				
				filename1.append(sector);
				filename1.append(".dat");
				filename2.append(paritySector);
				filename2.append(".dat");
				
				
				if(parityNameFlag){
					parityfile.append(sector);
					parityfile.append(paritySector);
					parityfile.append(".dat");
				}
				
				else{
					parityfile.append(paritySector);
					parityfile.append(sector);
					parityfile.append(".dat");
				}
				
				Path path1;
				Path path2;
				Path path3;
				File file1 = new File(filename1.toString());
				File file2 = new File(filename2.toString());
				File file3 = new File(parityfile.toString());
				byte[] data2;
				byte[] parityData;
				
				if(file1.exists() && !file1.isDirectory()){
					path1 = Paths.get(filename1.toString());
					data = Files.readAllBytes(path1);
				}
				
				else if(file2.exists() && !file2.isDirectory() && file3.exists() && !file3.isDirectory()) {
					path2 = Paths.get(filename2.toString());
					path3 = Paths.get(parityfile.toString());
					data2 = Files.readAllBytes(path2);
					parityData = Files.readAllBytes(path3);
					data = xor(data2,parityData);
				}
				
				else if(file3.exists() && !file3.isDirectory()){
					path3 = Paths.get(parityfile.toString());
					data = Files.readAllBytes(path3);
				}
		   	}
		   	
	   }
	   
	   catch(Exception ex){} 
	   
	   return data;
		
	}
	
	private static byte[] xor(byte[] a, byte[] b) {
  		byte[] result = new byte[8000];

  		for (int i = 0; i < result.length; i++) {
    	result[i] = (byte) (((int) a[i]) ^ ((int) b[i]));
  		}

  		return result;
	}
}
