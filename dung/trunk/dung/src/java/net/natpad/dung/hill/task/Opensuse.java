package net.natpad.dung.hill.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.natpad.dung.hill.HillSession;
import net.natpad.dung.hill.PipedStreamer;
import net.natpad.dung.module.task.LogLevel;
import net.natpad.dung.run.ProcessInputStreamer;
import net.natpad.dung.thread.IAction;
import net.natpad.dung.thread.ThreadPool;

public class Opensuse extends Task {

	public String repository;
	public String dir;

	public List<String> packageNames = new ArrayList<>();

	public String[] slots = new String[10];
	
	public void addPackage(String name) {
		packageNames.add(name);
	}

	@Override
	public void run(HillSession session) throws Exception {
		File outputDir = createOutputDir(session);

		String repoPageText = loadUrlAsText(new URL(repository));
		final String rpmlist[] = stripLoadableRpms(repoPageText);

		ThreadPool threadPool = new ThreadPool();
		log(LogLevel.INFO, "Installing:" + packageNames + " packages from "+repository);

		for (String name : packageNames) {

			
			threadPool.postAction(new IAction() {
				@Override
				public int runAction() {
					int slot = getSlot();
					try {
						URL realUrl = extractUrl(rpmlist, name);
						String filename = realUrl.getPath();
						int idx = filename.lastIndexOf('/');
						if (idx >= 0) {
							filename = filename.substring(idx + 1);
						}

						File archiveFile = new File(outputDir, filename);
						byte data[] = download(realUrl, archiveFile, slot);
						installRpm(outputDir, archiveFile);
					} catch (Exception e) {
						e.printStackTrace();
						return -1;
					} finally {
						 if (slot>=0) {
							synchronized (slots) {
								slots[slot] = null;
							}
						 }
					}
					return 0;
				}


			});
		}

		threadPool.finish();

		System.out.println();
		
		if (threadPool.exitcode != 0) {
			throw new RuntimeException("Opensuse Repository install returned " + threadPool.exitcode);
		}
	}

	private int getSlot() {
		synchronized (slots) {
			for(int idx=0; idx<slots.length; idx++) {
				if (slots[idx]==null) {
					slots[idx] = "---";
					return idx;
				}
			}
		}
		return -1;
	}
	
	private void notifySlot(int slotIndex) {
		StringBuilder buf = new StringBuilder();
		synchronized (slots) {
			for(int idx=0; idx<slots.length; idx++) {
				if (slots[idx]==null) {
					buf.append("         ");
				} else {
					String st = slots[idx]+"        ";
					st.substring(0, 8);
					buf.append(st).append(" ");
				}
			}
			String trim = buf.toString().trim();
			System.out.print((char) 0xD);
			System.out.print(trim);
			System.out.print(""+(char) 27+"[0K");
		}
	}

	
	private void installRpm(File outputDir, File archiveFile) throws IOException {
		Runtime runtime = Runtime.getRuntime();
		String args[] = new String[2];
		args[0] = "/usr/bin/rpm2cpio";
		args[1] = archiveFile.getCanonicalPath();
		Process rpm2cpioProcess = runtime.exec(args, null, outputDir);
//		System.out.println("started rpm2cpio");
		
		String args2[] = new String[3];
		args2[0] = "/bin/cpio";
		args2[1] = "-idmu";
		args2[2] = "--quiet";
		Process cpioProcess = runtime.exec(args2, null, outputDir);
//		System.out.println("started cpio");
		
		InputStream fromRpm2cpio = rpm2cpioProcess.getInputStream();
		new ProcessInputStreamer(rpm2cpioProcess.getErrorStream());
		OutputStream fromCpio = cpioProcess.getOutputStream();
		new PipedStreamer(fromRpm2cpio, fromCpio);
		new ProcessInputStreamer(cpioProcess.getErrorStream());
//		System.out.println("pipe created");
		while(cpioProcess!=null && fromRpm2cpio!=null) {
			boolean doSleep = true;
//			System.out.println("waking up");
			if (cpioProcess!=null) {
				try {
					if (cpioProcess.exitValue()==0) {
						cpioProcess = null;
						doSleep = false;
					}
				} catch(IllegalThreadStateException e) {
				}
			}
			if (rpm2cpioProcess!=null) {
				try {
					if (rpm2cpioProcess.exitValue()==0) {
						rpm2cpioProcess = null;
						doSleep = false;
					}
				} catch(IllegalThreadStateException e) {
				}
			}
			if (doSleep) {
				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	private File createOutputDir(HillSession session) throws IOException {
		File outputDir;
		if (dir == null) {
			outputDir = session.getWorkDir();
		} else {
			outputDir = session.createWorkPath(dir).toFile();
		}
		outputDir.mkdirs();
		return outputDir;
	}

	public byte[] download(URL url, File destFile, int slotIndex) {
		byte result[] = null;
		try {
			InputStream inputStream = null;
			RandomAccessFile outfile = null;

			URLConnection uc = url.openConnection();

			int contentLength = uc.getContentLength();
			boolean lengthKnown = true;
			if (contentLength < 0) {
				result = new byte[0x10000];
				lengthKnown = false;
			} else {
				result = new byte[contentLength];
			}
			int readSoFar = 0;
			if (destFile.exists()) {
				long destFileLength = destFile.length();
				if (lengthKnown) {
					inputStream = new FileInputStream(destFile);
					long toRead = destFileLength;
					toRead = destFileLength < contentLength ? destFileLength : contentLength;
					while (readSoFar < toRead) {
						int left = (int) (toRead - readSoFar);
						int cnt = inputStream.read(result, readSoFar, left);
						if (cnt > 0) {
							readSoFar += cnt;
						} else {
							break;
						}
					}
					inputStream.close();
					if (readSoFar == contentLength) {
						return result;
					}
				} else {
					readSoFar = (int) destFileLength;
				}

				inputStream = uc.getInputStream();
				inputStream.skip(readSoFar);

				outfile = new RandomAccessFile(destFile, "rw");
				outfile.seek(readSoFar);

			} else {
				outfile = new RandomAccessFile(destFile, "rw");
				inputStream = uc.getInputStream();
			}
			long startTime = System.currentTimeMillis();
			long nextTimeOut = startTime + 250;
			boolean stop = false;

			long badSpeedStart = 0;

			while (!stop) {
				int left = result.length;
				if (lengthKnown) {
					left = contentLength - readSoFar;
					if (left > 16384) {
						left = 16384;
					}
				}
				int readCount = inputStream.read(result, lengthKnown ? readSoFar : 0, left);
				if (readCount > 0) {
					readSoFar += readCount;
				} else {
					stop = true;
				}

				long fp = outfile.getFilePointer();
				left = (int) (readSoFar - fp);
				if (left > 0) {
					outfile.write(result, (int) fp, left);
				}

				long now = System.currentTimeMillis();
				if (now > nextTimeOut || stop) {
					int skipped = 0;
					while (nextTimeOut < now) {
						nextTimeOut += 250;
						skipped += 250;
					}

					long diff = (now - startTime) / 1000;
					if (diff < 1) {
						diff = 1;
					}
					long bytesPerSecond = readSoFar / diff;
					bytesPerSecond /= 1024;
					boolean reconnected = false;
					if (bytesPerSecond < 10 * 1024) {
						if (badSpeedStart == 0) {
							badSpeedStart = now;
						} else {
							if (skipped + (now - badSpeedStart) > 4000) {
								inputStream.close();
								uc = url.openConnection();
								inputStream = uc.getInputStream();
								inputStream.skip(readSoFar);
								reconnected = true;
							}
							badSpeedStart = 0;
						}
					} else {
						badSpeedStart = 0;
					}

					if (slotIndex>=0) {
						String txt = "";
						if (lengthKnown) {
							int per = 100 * readSoFar / contentLength;
							
							txt = readSoFar + "/" + contentLength + "[" + per + "%] ";
//							+ bytesPerSecond + "K/s" + (reconnected ? " [RECONNECTED]" : ""));
						} else {
							txt = readSoFar + " " + bytesPerSecond + "K/s";
//							System.out.print(
//									readSoFar + " " + bytesPerSecond + "K/s" + (reconnected ? " [RECONNECTED]" : ""));
						}
						slots[slotIndex] = txt;
						notifySlot(slotIndex);
					}

				}
			}

			outfile.close();

			if (!lengthKnown) {
				FileInputStream fis = new FileInputStream(destFile);
				result = new byte[(int) destFile.length()];
				fis.read(result);
				fis.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}


	private URL extractUrl(String rpmlist[], String packageName) throws Exception {

		ArrayList<String> matchedRpms = new ArrayList<String>();

		boolean do_debug = packageName.indexOf("debug") >= 0;

		for (String rpm : rpmlist) {
			int idx = rpm.lastIndexOf('/');
			String rpmName = rpm;
			// log("rpm="+rpm, Project.MSG_VERBOSE);
			if (idx >= 0) {
				rpmName = rpm.substring(idx + 1);
			}
			boolean rpm_is_debug = rpmName.indexOf("debug") >= 0;
			if (rpm_is_debug != do_debug) {
				continue;
			}
			if (rpmName.matches(packageName)) {
				matchedRpms.add(repository + "/" + rpmName);
			}
		}
		if (matchedRpms.size() == 1) {
			return new URL(matchedRpms.get(0));
		} else if (matchedRpms.size() > 1) {
			Collections.sort(matchedRpms);
			for (String mrpms : matchedRpms) {
				// session.log("::"+mrpms);
			}
			return new URL(matchedRpms.get(0));
		}
		throw new RuntimeException("no repro matches " + packageName);
	}

	private String loadUrlAsText(URL url) throws IOException {
		String result = null;
		for (int idx = 0; idx < 5; idx++) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				URLConnection connection = url.openConnection();
				connection.setReadTimeout(7000);
				connection.setRequestProperty("User-Agent",
						"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:45.0) Gecko/20100101 Firefox/45.0");
				InputStream stream = connection.getInputStream();

				byte bbuf[] = new byte[0x10000];
				while (true) {
					int cnt = stream.read(bbuf);
					if (cnt <= 0) {
						break;
					}
					baos.write(bbuf, 0, cnt);
				}
				idx = 5;
				result = new String(baos.toByteArray());
			} catch (SocketTimeoutException e) {
				System.out.println("[RECONNECTING]");
			} catch (Exception e) {
				if (idx == 4) {
					if (e instanceof IOException) {
						throw (IOException) e;
					}
					e.printStackTrace();
					return null;
				}
			}
		}
		return result;
	}

	public String[] stripLoadableRpms(String text) {
		int idxa = 0;
		ArrayList<String> list = new ArrayList<String>();
		while (true) {
			int idxb = text.indexOf("<a href=\"mingw32-", idxa);
			if (idxb <= 0) {
				idxb = text.indexOf("<a href=\"mingw64-", idxa);
				if (idxb <= 0) {
					break;
				}
			}
			idxb += 9;
			int idxc = text.indexOf("\"", idxb);
			if (idxc < 0) {
				break;
			}
			String raw = text.substring(idxb, idxc);
			if (raw.endsWith(".rpm") && raw.indexOf("evolution") < 0 && raw.indexOf("libqt4") < 0
					&& raw.indexOf("webkit") < 0 && raw.indexOf("mono") < 0 && raw.indexOf("pidgin") < 0
					&& raw.indexOf("texlive") < 0 && raw.indexOf("tomahawk") < 0) {
				list.add(raw);
			}
			idxa = idxb;
		}
		String result[] = new String[list.size()];
		return list.toArray(result);
	}

}
