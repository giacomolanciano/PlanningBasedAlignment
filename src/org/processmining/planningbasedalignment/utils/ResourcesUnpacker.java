package org.processmining.planningbasedalignment.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.planningbasedalignment.algorithms.PlanningBasedAlignment;

public class ResourcesUnpacker extends Thread {

	private UIPluginContext context;

	public ResourcesUnpacker(UIPluginContext context) {
		super();
		this.context = context;
	}

	@Override
	public void run() {
		try {
			unpackResourcesJar();
		} catch (Exception e) {
			e.printStackTrace();
			context.getFutureResult(0).cancel(true);
		}
	}

	private void unpackResourcesJar() throws IOException, URISyntaxException {

		Class<PlanningBasedAlignment> planningBasedAlignment = PlanningBasedAlignment.class;
		File jarFile = new File(planningBasedAlignment.getProtectionDomain().getCodeSource().getLocation().getPath());

		if (jarFile.isFile()) {  // Run with JAR file

			System.out.println("\t\t unpacking jar");

			JarFile jar = new JarFile(jarFile);
			Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
			while(entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				File f = new java.io.File(name);

				if (!name.endsWith(".class")) { //filter according to the path

					System.out.println("\t"+name);

					if (entry.isDirectory()) { // if its a directory, create it
						f.mkdir();
						continue;
					}
					java.io.InputStream is = jar.getInputStream(entry); // get the input stream
					java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
					while (is.available() > 0) {  // write contents of 'is' to 'fos'
						fos.write(is.read());
					}
					fos.close();
					is.close();

				}
			}
			jar.close();

		} else { // Run with IDE

			System.out.println("\t\t copying resources");

			String packageName = planningBasedAlignment.getPackage().getName();
			packageName = packageName.replaceAll("\\.", "/") + "/";

			final URL url = planningBasedAlignment.getResource("/" + packageName);
			if (url != null) {
				final File apps = new File(url.toURI());
				for (File app : apps.listFiles()) {
					if (!app.getName().endsWith(".class")) { //filter according to the path

						System.out.println(app);

						File dest = new File(app.getName());
						if (app.isDirectory())	// if its a directory, create it
							FileUtils.copyDirectory(app, dest);
						else
							FileUtils.copyFile(app, dest);
					}
				}
			}   
		}
	}
}
