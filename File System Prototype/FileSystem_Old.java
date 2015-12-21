import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class FileSystem {
	
	private Node fileSystemTree;
	public static StorageSystem fileStorageSystem;


	public FileSystem(int raid) {
		fileStorageSystem = new StorageSystem(raid);
		
		File f = new File("disk.txt");
		File f1 = new File("disk1.txt");
		File f2 = new File("disk2.txt");
		File f3 = new File("disk3.txt");
		
		if(f.exists() && !f.isDirectory()) {
			fileSystemTree = fileStorageSystem.Read(0);
		}
		
		else if(f1.exists() && f2.exists() && f3.exists() && (raid == 0 || raid == 4 || raid == 5)){
			fileSystemTree = fileStorageSystem.Read(0);
		}
		
		else {
			fileSystemTree = new Node("root");
			fileStorageSystem.Write(fileSystemTree);
		}
	}
	
	public void dir() {
		Node currDir = fileSystemTree;
		
		String[] listOfContents = new String[currDir.children.size()];
		
		listOfContents = currDir.children.keySet().toArray(listOfContents);
		
		Arrays.sort(listOfContents);
		
		if(listOfContents.length > 0){
		
			for(String S : listOfContents){
				System.out.println(S);
			}
		
		}
		
		else{
			System.out.println("Directory is empty");
		}
	}
	
	public void mkdir(String name) {
		Node currDir = fileSystemTree;
		Node child = currDir.createChild(name);
		if(child != null){
			fileStorageSystem.Write(child);
		}
		
		else{
			System.out.println("Directory name already exists");
		}
	}
	
	public void rmdir(String name) {
		Node currDir = fileSystemTree;
		Node child = currDir.getChild(name);
		if(child != null){
		
			if (!(child.children.size() > 0)){
				int sector = child.mySector;
				currDir.children.remove(name);	
				fileStorageSystem.Delete(sector);
			}
			
			else{
				System.out.println("Directory is not empty cannot delete");
			}
			
		}
		
		else{
			System.out.println("Directory name does not exist");
		}
	}
	
	public void cd(String path){
		Node currDir = fileSystemTree;
		
		if(path.equals("..") && !currDir.name.equals("root")){
			fileSystemTree = currDir.parent;
		}
		
		else{
		
			String[] names = path.split("/");
	
			int i = 0;	
			while(i < names.length && currDir != null){
				currDir = currDir.getChild(names[i]);
				i++;
			}
		
			if(currDir == null){
				System.out.println("Path does not exist");
			}
		
			else{
				fileSystemTree = currDir;
			}
		}
	
	}
	
	public void externalcopy(String path){
	
		try{
			Path filepath = Paths.get(path);
			byte[] data = Files.readAllBytes(filepath);
		
			Node currDir = fileSystemTree;
			Node file = currDir.createFile(path,data);
			if(file != null){
				fileStorageSystem.Write(file);
			}
		
			else{
				System.out.println("File name already exists");
			}
		}
		catch(Exception ex){}
		
	}
	
	public void del(String name) {
		Node currDir = fileSystemTree;
		Node file = currDir.getChild(name);
		if(file != null){
			int sector = file.mySector;
			currDir.children.remove(name);	
			fileStorageSystem.Delete(sector);

		}
		
		else{
			System.out.println("Filename does not exist");
		}
	}
	
	public void copy(String name, String path) {
		Node tempDir = fileSystemTree;
		Node tempFile = tempDir.getChild(name);
		if(tempFile != null){
			this.cd(path);
			StringBuilder copyname = new StringBuilder("copy_");
			copyname.append(name);
			Node file = fileSystemTree.createFile(copyname.toString(),tempFile.data);
			if(file != null){
				fileStorageSystem.Write(file);
			}
			fileSystemTree = tempDir;
		}
		
		else{
			System.out.println("Filename does not exist");
		}
	
	}
	
}


class Node implements Serializable{

	public String name;
	public int mySector;
	public Node parent;
	public int parentSector;
	public HashMap<String, Node> children;
	public byte[] data;
	
	public Node(String n){
	
		name = new String(n);
		children = new HashMap<String, Node>();
		
		if(n.equals("root")){
			parent = null;
			mySector = 0;
			parentSector = -1;
		}
	
	}
	
	public Node(String n, byte[] d){
		name = new String(n);
		data = d;
		children = new HashMap<String, Node>();
	}
	
	public Node getChild(String name){
		if (this.children.containsKey(name)){
			return this.children.get(name);
		}
		return null;
	}

	
	public Node createChild(String name){
		
		if (!this.children.containsKey(name)){
			Node newNode = new Node(name);
			newNode.parent = this;
			newNode.parentSector = this.mySector;
			this.children.put(name, newNode);
			
			return newNode;
		}
		
		else{
			return null;
		}
	}
	
	public Node createFile(String name, byte[] data){
		
		if (!this.children.containsKey(name)){
			Node newFile = new Node(name,data);
			newFile.parent = this;
			newFile.parentSector = this.mySector;
			this.children.put(name, newFile);
			return newFile;
		}
		
		else{
			return null;
		}
	}

}



class StorageSystem {
	
	//public HashMap<Integer,Node> blocks;
	public Node[] blocks;
	public LinkedList<Integer> freeBlocks;
	private int raid;
	private int MAX_MEMORY;
	private Integer size;
	
	public StorageSystem(int r) {
		raid = r;
		MAX_MEMORY = 32*32;
		size = 0;
		//blocks = new HashMap<Integer,Node>();
		blocks = new Node[MAX_MEMORY];
		freeBlocks = new LinkedList<Integer>();
	}
	
	
	public void Write() {

		try {
		
			if(raid == 1 || (raid != 0 && raid != 4 && raid != 5)){
				FileOutputStream fout = new FileOutputStream("disk.txt");
				ObjectOutputStream oos = new ObjectOutputStream(fout);   
				oos.writeObject(freeBlocks);
				oos.writeObject(blocks);
				oos.close();
				fout.close();
			}
			
			if(raid == 1){
				FileOutputStream fout1 = new FileOutputStream("disk_mirrored.txt");
				ObjectOutputStream oos1 = new ObjectOutputStream(fout1);
				oos1.writeObject(freeBlocks);
				oos1.writeObject(blocks);
				oos1.close();
				fout1.close();
			}
			
			if(raid == 0){
				
				FileOutputStream fout1 = new FileOutputStream("disk1.txt");
				ObjectOutputStream oos1 = new ObjectOutputStream(fout1);
				
				oos1.writeObject(size);
				oos1.writeObject(freeBlocks);
				
				for(int i = 0; i < size; i = i+3){
					oos1.writeObject(blocks[i]);
				}
				
				oos1.close();
				fout1.close();
				
				FileOutputStream fout2 = new FileOutputStream("disk2.txt");
				ObjectOutputStream oos2 = new ObjectOutputStream(fout2);
				
				for(int i = 1; i < size; i = i+3){
					oos2.writeObject(blocks[i]);
				}
				
				oos2.close();
				fout2.close();
				
				FileOutputStream fout3 = new FileOutputStream("disk3.txt");
				ObjectOutputStream oos3 = new ObjectOutputStream(fout3);
				
				for(int i = 2; i < size; i = i+3){
					oos3.writeObject(blocks[i]);
				}
				
				oos3.close();
				fout3.close();
			}
			
	   	}
	   	
	   	catch(Exception ex){}
	}
	
	
	public void Write(Node obj) {
		
		if(freeBlocks.size() > 0){
			int sector = freeBlocks.remove();
			size++;
			obj.mySector = sector;
			//blocks.put(sector, obj);
			blocks[sector] = obj;
		}
		
		
		else{
			//int sector = blocks.size();
			int sector = size;
			size++;
			obj.mySector = sector;
			//blocks.put(sector, obj);
			blocks[sector] = obj;
		}

		try {
			if(raid == 1 || (raid != 0 && raid != 4 && raid != 5)){
				FileOutputStream fout = new FileOutputStream("disk.txt");
				ObjectOutputStream oos = new ObjectOutputStream(fout);   
				oos.writeObject(freeBlocks);
				oos.writeObject(blocks);
				oos.close();
				fout.close();
	
				if(raid == 1){
					FileOutputStream fout1 = new FileOutputStream("disk_mirrored.txt");
					ObjectOutputStream oos1 = new ObjectOutputStream(fout1);
					oos1.writeObject(freeBlocks);
					oos1.writeObject(blocks);
					oos1.close();
					fout1.close();
				}
			}
			
			if(raid == 0){
				
				FileOutputStream fout1 = new FileOutputStream("disk1.txt");
				ObjectOutputStream oos1 = new ObjectOutputStream(fout1);
				
				oos1.writeObject(size);
				oos1.writeObject(freeBlocks);
				
				for(int i = 0; i < size; i = i+3){
					oos1.writeObject(blocks[i]);
				}
				
				oos1.close();
				fout1.close();
				
				FileOutputStream fout2 = new FileOutputStream("disk2.txt");
				ObjectOutputStream oos2 = new ObjectOutputStream(fout2);
				
				for(int i = 1; i < size; i = i+3){
					oos2.writeObject(blocks[i]);
				}
				
				oos2.close();
				fout2.close();
				
				FileOutputStream fout3 = new FileOutputStream("disk3.txt");
				ObjectOutputStream oos3 = new ObjectOutputStream(fout3);
				
				for(int i = 2; i < size; i = i+3){
					oos3.writeObject(blocks[i]);
				}
				
				oos3.close();
				fout3.close();
			}
			
	   	}
	   	
	   	catch(Exception ex){}
	}
	
	public Node Read(int sector) {
		
		try{
			if(raid == 1 || (raid != 0 && raid != 4 && raid != 5)){
		  		FileInputStream fin = new FileInputStream("disk.txt");
		  	 	ObjectInputStream ois = new ObjectInputStream(fin);
		  		freeBlocks = (LinkedList<Integer>) ois.readObject();
		   		//blocks = (HashMap<Integer,Node>) ois.readObject();
		   		blocks = (Node[]) ois.readObject();
		   		ois.close();
		   	}
		   	
		   	else if(raid == 0){
		   		FileInputStream fin1 = new FileInputStream("disk1.txt");
		   		FileInputStream fin2 = new FileInputStream("disk2.txt");
		   		FileInputStream fin3 = new FileInputStream("disk3.txt");
		  	 	ObjectInputStream ois1 = new ObjectInputStream(fin1);
		  	 	ObjectInputStream ois2 = new ObjectInputStream(fin2);
		  	 	ObjectInputStream ois3 = new ObjectInputStream(fin3);
		  	 	
		  	 	blocks = new Node[MAX_MEMORY];
		  	 	size = (Integer) ois1.readObject();
		  		freeBlocks = (LinkedList<Integer>) ois1.readObject();


				for(int i = 0; i < size; i = i+3){
					blocks[i] = (Node) ois1.readObject();
				}
				
				for(int i = 1; i < size; i = i+3){
					blocks[i] = (Node) ois2.readObject();
				}
				
				for(int i = 2; i < size; i = i+3){
					blocks[i] = (Node) ois3.readObject();
				}


		   		ois1.close();
				fin1.close();
				ois2.close();
				fin2.close();
				ois3.close();
				fin3.close();
		   	
		   	}
		   	
		   	
	   }
	   
	   catch(Exception ex){} 

	   //return blocks.get(sector);
	   return blocks[sector];
		
	}
	
	public void Delete(int sector){
		//blocks.remove(sector);
		blocks[sector] = null;
		size--;
		freeBlocks.add(sector);
		this.Write();
	}
	
	private byte[] xor(byte[] a, byte[] b) {
 		byte[] result = new byte[Math.min(a.length, b.length)];

  		for (int i = 0; i < result.length; i++) {
    	result[i] = (byte) (((int) a[i]) ^ ((int) b[i]));
  	}

  return result;
}
}

/*
The best way to do it is to use ApacheUtils:

To Serialize:

byte[] data = SerializationUtils.serialize(yourObject);
deserialize:

YourObject yourObject = (YourObject) SerializationUtils.deserialize(byte[] data)

*/
