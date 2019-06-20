package download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import javafx.application.Platform;

public class DownloadThread {

	private MyDownLoadThread[] threads = null;
    private String filepath = null;
    private String filename = null;
    private String tmpfilename = null;

    private int threadNum = 0;//线程数
    private long fileLength = 0l;
    private long threadLength = 0l;
    private long[] startPos;//保留每个线程下载数据的起始位置。
    private long[] endPos;//保留每个线程下载数据的截止位置。

    private boolean bool = false;

    private URL url = null;

    public DownloadThread(String filepath, int threadNum) {
        this.filepath = filepath;
        this.threadNum = threadNum;
        startPos = new long[this.threadNum];
        endPos = new long[this.threadNum];
        threads = new MyDownLoadThread[this.threadNum];
    }

    public void downloadPart() {

        File file = null;
        File tmpfile = null;
        HttpURLConnection httpcon = null;

        //在请求url内获取文件资源的名称
        filename = filepath.substring(filepath.lastIndexOf('/') + 1, filepath
                .contains("?") ? filepath.lastIndexOf('?') : filepath.length());
        if("".equalsIgnoreCase(this.filename)){  
            this.filename = UUID.randomUUID().toString();  
        }
        tmpfilename = filename + "_tmp";//临时文件,用来记录断点

        try {
            url = new URL(filepath);
            httpcon = (HttpURLConnection) url.openConnection();

            setHeader(httpcon);
            fileLength = httpcon.getContentLengthLong();//获取请求资源的总长度。

            file = new File(filename);
            tmpfile = new File(tmpfilename);

            threadLength = fileLength / threadNum;//每个线程需下载的资源大小。
            
            System.out.println("文件名: " + filename + "  " + "文件大小：  "
                    + fileLength + " 单个线程下载文件的大小：  " + threadLength);

            if (file.exists() && file.length() ==fileLength) {
                System.out.println("文件已存在！");
                return;
            } else {
                setBreakPoint(startPos, endPos, tmpfile);//设置断点
                for(int i=0;i<threadNum;i++) {
                	threads[i] = new MyDownLoadThread(startPos[i], endPos[i],this, i, tmpfile);//为每个线程分配任务
                	threads[i].start();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFilepath() {
		return filepath;
	}

	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}

	//暂停线程
    public void stopThread() {
    	for(int i=0;i<threadNum;i++) {
    		threads[i].stop();
    	}
    	System.out.println("下载暂停");
    }
    
    
    //断点设置方法，当有临时文件时，直接在临时文件中读取上次下载中断时的断点位置。没有临时文件，即第一次下载时，重新设置断点。 
    private void setBreakPoint(long[] startPos, long[] endPos, File tmpfile) {
        RandomAccessFile rantmpfile = null;
        try {
            if (tmpfile.exists()) {
                System.out.println("下载继续！");
                rantmpfile = new RandomAccessFile(tmpfile, "rw");
                for (int i = 0; i < threadNum; i++) {
                	//从临时文件中读取每个线程的断点起始位置与大小
                    rantmpfile.seek(16 * i + 16);
                    startPos[i] = rantmpfile.readLong();

                  //从临时文件中读取每个线程的断点终点位置与大小
                    rantmpfile.seek(16 * (i + 5000) + 16);
                    endPos[i] = rantmpfile.readLong();

                }
            } else {
                rantmpfile = new RandomAccessFile(tmpfile, "rw");
                
                for (int i = 0; i < threadNum; i++) {
                    startPos[i] = threadLength * i;
                    if (i == threadNum - 1) {
                        endPos[i] = fileLength;
                    } else {
                        endPos[i] = threadLength * (i + 1) - 1;
                    }

                    //记录每个线程断点起始位置与大小
                    rantmpfile.seek(16 * i + 16);
                    rantmpfile.writeLong(startPos[i]);

                    //记录每个线程断点终点位置与大小
                    rantmpfile.seek(16 * (i + 5000) + 16);
                    rantmpfile.writeLong(endPos[i]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rantmpfile != null) {
                    rantmpfile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    //下载线程 
    class MyDownLoadThread extends Thread {

        private long startPos;
        private long endPos;
        private DownloadThread task = null;
        private RandomAccessFile downloadfile = null;
        private int id;
        private File tmpfile = null;
        private RandomAccessFile rantmpfile = null;
        private File file;
       
        public MyDownLoadThread(long startPos, long endPos,
                DownloadThread task, int id, File tmpfile) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.task = task;
            this.tmpfile = tmpfile;
            try {
                this.downloadfile = new RandomAccessFile(this.task.filename,
                        "rw");
                this.rantmpfile = new RandomAccessFile(this.tmpfile, "rw");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            this.id = id;
            file = new File(task.filename);
        }
     
        @Override
        public void run() {

            HttpURLConnection httpcon = null;
            InputStream is = null;
            int length = 0;

            System.out.println("thread " + id + " 开始下载！");

            while (true) {
            	try {
                    httpcon = (HttpURLConnection) task.url.openConnection();
                    setHeader(httpcon);
                    
                    //设置超时时间
                    httpcon.setReadTimeout(200000);//读取数据的超时设置
                    httpcon.setConnectTimeout(200000);//连接的超时设置

                    if (startPos < endPos) {
                        
                        //向服务器请求指定区间段的数据
                        httpcon.setRequestProperty("Range", "bytes=" + startPos
                                + "-" + endPos);

                        downloadfile.seek(startPos);

                        if (httpcon.getResponseCode() != HttpURLConnection.HTTP_OK
                                && httpcon.getResponseCode() != HttpURLConnection.HTTP_PARTIAL) {
                            this.task.bool = true;
                            httpcon.disconnect();
                            downloadfile.close();
                            System.out.println("thread " + id
                                    + " 已完成");
                            break;
                        }

                        is = httpcon.getInputStream();//获取服务器返回的资源流
                        long count = 0l;
                        byte[] buf = new byte[1024];

                        while (!this.task.bool && (length = is.read(buf)) != -1) {
                            count += length;
                            downloadfile.write(buf, 0, length);
                            
                            //更新每个线程下载资源的起始位置，并写入临时文件
                            startPos += length;
                            rantmpfile.seek(8 * id + 8);
                            rantmpfile.writeLong(startPos);
                            //进度条
                            Platform.runLater(new Runnable() {
								
								@Override
								public void run() {
									// TODO Auto-generated method stub
									Number number = (double)(endPos-startPos);
									Main.progressBars[id].setProgress(1-number.doubleValue()/threadLength);
								}
							});
                        }
                        
                        //关闭流
                        is.close();
                        httpcon.disconnect();
                        downloadfile.close();
                        rantmpfile.close();
                    }
                    System.out.println("thread " + id + " 下载完成");
                    if(file.exists() && file.length()== fileLength) {
                    	if(tmpfile.exists()) {
                    		System.out.println("下载完成");
                        	tmpfile.delete();
                    	}
                    }
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //HTTP头文件设置
    private void setHeader(HttpURLConnection con) {
        con.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.3) Gecko/2008092510 Ubuntu/8.04 (hardy) Firefox/3.0.3");
        con.setRequestProperty("Accept-Language", "en-us,en;q=0.7,zh-cn;q=0.3");
        con.setRequestProperty("Accept-Encoding", "aa");
        con.setRequestProperty("Accept-Charset",
                "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        con.setRequestProperty("Keep-Alive", "300");
        con.setRequestProperty("Connection", "keep-alive");
        con.setRequestProperty("If-Modified-Since",
                "Fri, 02 Jan 2009 17:00:05 GMT");
        con.setRequestProperty("If-None-Match", "\"1261d8-4290-df64d224\"");
        con.setRequestProperty("Cache-Control", "max-age=0");
        con.setRequestProperty("Referer",
                "http://www.skycn.com/soft/14857.html");
    }
}
