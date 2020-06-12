package test.net;

import jx.zero.*;
import jx.zero.debug.*;

import java.io.OutputStream;
import java.io.DataInputStream;

import java.net.Socket;
import jx.fs.*;
import java.util.*;

class TCPWebWorker {
    private final static boolean debug = true; 
    final static int METHOD_UNDEF = 0;
    final static int METHOD_GET = 1;
    final static int METHOD_POST = 2;
    int method = 0;
    int contentsLength = -1;
    MemoryManager memoryManager;
    Memory buffer1;
    Memory buffer2;
    FS fs;

    public static void init(Naming naming, String[] argv, Object[] objs)  throws Exception {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));

	Socket s= (Socket) objs[0];
	processRequest(s, (FS) objs[1]);
	
	DomainManager domainManager = (DomainManager) naming.lookup("DomainManager");
	domainManager.terminateCaller();	
    }

    public static void processRequest(Socket sock, FS fs)  throws Exception {
	new TCPWebWorker(sock, fs);
    }

    TCPWebWorker(Socket sock, FS fs)  throws Exception {
	this.fs = fs;

	memoryManager = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
	buffer1 = memoryManager.alloc(4096);
        buffer2 = memoryManager.alloc(4096);

	OutputStream ostream = sock.getOutputStream();
	DataInputStream requestStream = new DataInputStream(sock.getInputStream());
	String file = parse(requestStream);
	if (debug) Debug.out.println("HTTPServer: GET file " + file);

        switch (file) {
            case "stream.exe":
                {
                    byte[] data = new byte[1024];
                    // stream data
                    ostream.write(constructHeader(file, 1024*1024).getBytes());
                    for(int i = 0; i < 1024; i++) {
                        ostream.write(data);
                    }       
                    break;
                }
            case "dummy.html":
                {
                    byte[] data = new byte[1024];
                    // stream data
                    ostream.write(constructHeader(file, 1024).getBytes());
                    ostream.write(data);
                    break;
                }
            case "mini.html":
                {
                    byte[] data = new byte[1];
                    // stream data
                    ostream.write(constructHeader(file, 1).getBytes());
                    ostream.write(data);
                    break;
                }
            default:
                {
                    byte[] data = readFile("/"+file);
                    ostream.write(constructHeader(file, data.length).getBytes());
                    ostream.write(data);
                    if (debug) Debug.out.println("HTTPServer: reply sent.");
                    break;
                }
        }
	ostream.flush();
	sock.close();
    }
    
    byte[] readFile(String name)  {
	try {
	    Inode inode = fs.lookup(name);
	    int l = inode.getLength();
            Debug.out.println("l: " + l);
	    inode.read(buffer1, 0,  l);
            inode.read(buffer2, 1, l);
	    byte data[] = new byte[l];
	   buffer1.copyToByteArray(data, 0, 0, 512*8);
            buffer2.copyToByteArray(data, 512*8, 0, l-512*8);
            for(int i = 0; i < l; i++){
                Debug.out.print((char)data[i]);
            }
	    return data;
	} catch (InodeIOException | InodeNotFoundException | NoDirectoryInodeException | NoFileInodeException | NotExistException | PermissionException ex) {
	      Debug.out.println(ex.getMessage());
	    return constructErrorHeader().getBytes();
	}
    }

    String parse(DataInputStream fromClient) throws Exception {
        String fileName = null;
        String line;
        try {
            while (! (line = fromClient.readLine()).equals("")) {
                if (debug) Debug.out.println("REQ: " + line);
                StringTokenizer tokenizer = new StringTokenizer(line, " ");
                if (!tokenizer.hasMoreTokens()) continue;
                String tok = tokenizer.nextToken();
                switch (tok) {
                    case "GET":
                        method = METHOD_GET;
                        fileName = tokenizer.nextToken();
                        if (fileName.endsWith("/")) {
                            fileName = fileName + getProperty_DefaultFile();
                        } else {
                            fileName = fileName.substring(1);
                        }   break;
                    case "POST":
                        method = METHOD_POST;
                        fileName = tokenizer.nextToken();
                        fileName = fileName.substring(1);
                        Debug.out.println("FILENAME: "+fileName);
                        break;
                    case "Content-length:":
                        contentsLength = Integer.parseInt(tokenizer.nextToken());
                        break;
                    default:
                        break;
                }
 
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }
    static public String constructHeader(String fileName, String fileType, int fileLength) {
        /*
	  return  "HTTP/1.0 200 OK\n" + "Allow: GET\nMIME-Version: 1.0\n" + "Server : A Java HTTP Server\n" +
	  "Content-Type: " + fileType + "\n" + "Content-Length: " + fileLength + "\n\n";
        */
        return  "HTTP/1.0 200 OK\n"
            + "Content-Length: " + fileLength + "\nContent-Type: " + fileType + "\n\n";
	
    }
    
    static public String constructHeader(String fileName, int fileLength) {
        String fileType;
        fileType = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        fileType = getProperty_MimeTypes(fileType);
        return constructHeader(fileName, fileType, fileLength);
    }
    static public String constructErrorHeader() {
          return  "HTTP/1.0 404 File not found\n" + "Allow: GET\n" + "MIME-Version: 1.0\n"
        + "Server : JX HTTP Server\n" +
            "\n\n <H1>404 File not Found</H1>\n";
    }

    static String getProperty_DefaultFile() {
	return "index.html";
    }
    static String getProperty_MimeTypes(String fileType) {
	if (fileType.equals("html")) return "text/html";
	if (fileType.equals("htm")) return "text/html";
	return fileType; 
    }
}

