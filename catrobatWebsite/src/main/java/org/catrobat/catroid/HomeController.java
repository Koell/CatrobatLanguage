package org.catrobat.catroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.XmlHeader;
import org.catrobat.catroid.translator.Translator;
import org.catrobat.catroid.yaml.YamlProject;
import org.catrobat.catroid.yaml.YamlSprite;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class HomeController {

	private class UploadException extends Exception {

		private static final long serialVersionUID = 1L;
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Model model, HttpServletRequest request) {
		YamlProject project;
		try {
			project = getProject(request);
		} catch (UploadException e) {
			return "home";
		}

		setModelAttributes(model, request, project);

		return "home";
	}

	private Map<String, String> createrHeaderMap(XmlHeader header) {
		Map<String, String> headerMap = new TreeMap<String, String>();
		headerMap.put("programName", header.getProgramName());
		headerMap.put("description", header.getDescription());

		headerMap.put("screenWidth",
				(new Integer(header.getVirtualScreenWidth())).toString());
		headerMap.put("screenHeight",
				(new Integer(header.getVirtualScreenHeight())).toString());

		headerMap.put("catrobatLanguageVersion",
				(new Double(header.getCatrobatLanguageVersion())).toString());

		headerMap.put("applicationBuildName", header.getApplicationBuildName());
		headerMap.put("applicationBuildNumber",
				(new Integer(header.getApplicationBuildNumber())).toString());
		headerMap.put("applicationName", header.getApplicationName());
		headerMap.put("applicationVersion", header.getApplicationVersion());
		headerMap.put("dateTimeUpload", header.getDateTimeUpload());
		headerMap.put("deviceName", header.getDeviceName());
		headerMap.put("mediaLicense", header.getMediaLicense());
		headerMap.put("platform", header.getPlatform());
		headerMap.put("platformVersion",
				(new Integer(header.getPlatformVersion())).toString());
		headerMap.put("programLicense", header.getProgramLicense());
		headerMap.put("remixOf", header.getRemixOf());
		headerMap.put("tags", header.getTags());
		headerMap.put("url", header.getUrl());
		headerMap.put("userHandle", header.getUserHandle());

		List<String> emptyValues = new ArrayList<String>();
		for (String item : headerMap.keySet())
			if (headerMap.get(item) == null || headerMap.get(item) == "") {
				emptyValues.add(item);
			}
		for (String item : emptyValues)
			headerMap.remove(item);

		return headerMap;
	}

	private Map<String, String> createLooksMap(List<LookData> looks,
			HttpServletRequest request) {
		Map<String, String> looksMap = new TreeMap<String, String>();

		for (LookData item : looks) {
			looksMap.put(item.getName(),
					request.getSession().getAttribute("projectFolder")
							+ "/images/" + item.getFileName());
		}
		return looksMap;
	}

	private Map<String, String> createSoundsMap(List<SoundInfo> sounds,
			HttpServletRequest request) {
		Map<String, String> soundsMap = new TreeMap<String, String>();

		for (SoundInfo item : sounds) {
			soundsMap.put(item.getName(),
					request.getSession().getAttribute("projectFolder")
							+ "/sounds/" + item.getFileName());
		}
		return soundsMap;
	}

	private String escape(String str) {
		// TODO: add all characters which we need to escape
		str = str.replace(" ", "");
		str = str.replace("\"", "");
		str = str.replace("&", "");
		str = str.replace("?", "");
		return str;
	}

	private YamlProject getProject(HttpServletRequest request)
			throws UploadException {
		File xmlProject = new File((String) request.getSession().getAttribute(
				"fullPathToProject")
				+ "\\code.xml");
		if (!xmlProject.exists())
			throw new UploadException();
		YamlProject project = new YamlProject(Translator.getInstance()
				.loadProjectFromXML(xmlProject));
		return project;
	}

	private void setModelAttributes(Model model, HttpServletRequest request,
			YamlProject project) {
		model.addAttribute("programName", project.getHeader().getProgramName());

		model.addAttribute("xmlHeader", createrHeaderMap(project.getHeader()));
		model.addAttribute("variables", project.getProjectVariables());

		Map<String, ObjectPresentation> objectMap = new TreeMap<String, ObjectPresentation>();
		ObjectPresentation buf;
		YamlSprite sprite;

		for (String item : project.getObjects().keySet()) {
			sprite = project.getObjects().get(item);
			buf = new ObjectPresentation();
			buf.setName(item);
			buf.setLooks(createLooksMap(sprite.getLooks(), request));
			buf.setSounds(createSoundsMap(sprite.getSounds(), request));
			buf.setCode(sprite.getScripts());
			buf.setVariables(sprite.getVariables());

			objectMap.put(escape(item), buf);
		}

		model.addAttribute("objects", objectMap);
	}

	@RequestMapping(value = "/*", method = RequestMethod.POST)
	public String Upload(
			@RequestParam(value = "file", required = true) MultipartFile file,
			Model model, HttpServletRequest request) throws IOException {

		if (file == null)
			return home(model, request);
		if (!isCatrobatFile(file.getOriginalFilename())) {
			return error(
					model,
					"Illegal file!",
					"File you tried to upload is not .catrobat file. Please, upload catrobat project (e. g.\"project.catrobat\")");

		}

		String appFolder = request.getSession().getServletContext()
				.getRealPath("/");
		String uploadFolder = getUploadFolder(request);

		try {
			createProjectDir(appFolder, uploadFolder);
		} catch (UploadException e1) {
			return error(
					model,
					"Cannot create directory for project",
					"Oops, it seems we have some internal error. Please, try one more time and do mot hurry.");
		}

		String filePath = appFolder + uploadFolder + "\\"
				+ file.getOriginalFilename();

		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(new File(filePath));
			outputStream.write(file.getBytes());
			outputStream.close();
		} catch (Exception e) {
			return error(model, "Cannot save file", e.getStackTrace()
					.toString());
		}

		setPathsToSesisionAttributes(request, appFolder, uploadFolder);

		ZipFile zip;
		try {
			zip = new ZipFile(filePath);
			zip.extractAll(appFolder + uploadFolder);
		} catch (ZipException e) {
			return error(model, "Cannot unzip project file", e.getStackTrace()
					.toString());
		}

		return home(model, request);
	}

	private void setPathsToSesisionAttributes(HttpServletRequest request,
			String appFolder, String uploadFolder) {
		request.getSession().setAttribute("projectFolder",
				uploadFolder.replace("\\", "/"));
		request.getSession().setAttribute("fullPathToProject",
				appFolder + uploadFolder);
	}

	private void createProjectDir(String appFolder, String uploadFolder)
			throws IOException, UploadException {
		File projectDir = new File(appFolder + uploadFolder);
		if (projectDir.exists())
			FileUtils.deleteDirectory(projectDir);
		if (!projectDir.mkdir())
			throw new UploadException();
	}

	private boolean isCatrobatFile(String name) {
		return (name.substring(name.length() - 8, name.length()).toLowerCase()
				.equals("catrobat"));

	}

	private String getUploadFolder(HttpServletRequest request) {
		String uploadFolder = null;
		if (request.getSession().getAttribute("projectFolder") != null)
			uploadFolder = ((String) request.getSession().getAttribute(
					"projectFolder")).replace("/", "\\");
		else
			uploadFolder = "resources\\upload\\" + UUID.randomUUID().toString();
		return uploadFolder;
	}

	private String error(Model model, String title, String msg) {
		model.addAttribute("errorTitle", title);
		model.addAttribute("errorMsg", msg);
		return "error";
	}
}
