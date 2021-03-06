/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.translator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.BroadcastScript;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.WhenScript;
import org.catrobat.catroid.content.XmlHeader;
import org.catrobat.catroid.content.bricks.BrickBaseType;
import org.catrobat.catroid.content.bricks.BroadcastBrick;
import org.catrobat.catroid.content.bricks.BroadcastReceiverBrick;
import org.catrobat.catroid.content.bricks.BroadcastWaitBrick;
import org.catrobat.catroid.content.bricks.ChangeBrightnessByNBrick;
import org.catrobat.catroid.content.bricks.ChangeGhostEffectByNBrick;
import org.catrobat.catroid.content.bricks.ChangeSizeByNBrick;
import org.catrobat.catroid.content.bricks.ChangeVariableBrick;
import org.catrobat.catroid.content.bricks.ChangeVolumeByNBrick;
import org.catrobat.catroid.content.bricks.ChangeXByNBrick;
import org.catrobat.catroid.content.bricks.ChangeYByNBrick;
import org.catrobat.catroid.content.bricks.ClearGraphicEffectBrick;
import org.catrobat.catroid.content.bricks.ComeToFrontBrick;
import org.catrobat.catroid.content.bricks.ForeverBrick;
import org.catrobat.catroid.content.bricks.GlideToBrick;
import org.catrobat.catroid.content.bricks.GoNStepsBackBrick;
import org.catrobat.catroid.content.bricks.HideBrick;
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick;
import org.catrobat.catroid.content.bricks.IfLogicElseBrick;
import org.catrobat.catroid.content.bricks.IfLogicEndBrick;
import org.catrobat.catroid.content.bricks.IfOnEdgeBounceBrick;
import org.catrobat.catroid.content.bricks.LegoNxtMotorActionBrick;
import org.catrobat.catroid.content.bricks.LegoNxtMotorStopBrick;
import org.catrobat.catroid.content.bricks.LegoNxtMotorTurnAngleBrick;
import org.catrobat.catroid.content.bricks.LegoNxtPlayToneBrick;
import org.catrobat.catroid.content.bricks.LoopBeginBrick;
import org.catrobat.catroid.content.bricks.LoopEndBrick;
import org.catrobat.catroid.content.bricks.LoopEndlessBrick;
import org.catrobat.catroid.content.bricks.MoveNStepsBrick;
import org.catrobat.catroid.content.bricks.NextLookBrick;
import org.catrobat.catroid.content.bricks.NoteBrick;
import org.catrobat.catroid.content.bricks.PlaceAtBrick;
import org.catrobat.catroid.content.bricks.PlaySoundBrick;
import org.catrobat.catroid.content.bricks.PointInDirectionBrick;
import org.catrobat.catroid.content.bricks.PointToBrick;
import org.catrobat.catroid.content.bricks.RepeatBrick;
import org.catrobat.catroid.content.bricks.SetBrightnessBrick;
import org.catrobat.catroid.content.bricks.SetGhostEffectBrick;
import org.catrobat.catroid.content.bricks.SetLookBrick;
import org.catrobat.catroid.content.bricks.SetSizeToBrick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.content.bricks.SetVolumeToBrick;
import org.catrobat.catroid.content.bricks.SetXBrick;
import org.catrobat.catroid.content.bricks.SetYBrick;
import org.catrobat.catroid.content.bricks.ShowBrick;
import org.catrobat.catroid.content.bricks.SpeakBrick;
import org.catrobat.catroid.content.bricks.StopAllSoundsBrick;
import org.catrobat.catroid.content.bricks.TurnLeftBrick;
import org.catrobat.catroid.content.bricks.TurnRightBrick;
import org.catrobat.catroid.content.bricks.WaitBrick;
import org.catrobat.catroid.content.bricks.WhenBrick;
import org.catrobat.catroid.content.bricks.WhenStartedBrick;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.formulaeditor.UserVariablesContainer;
import org.catrobat.catroid.yaml.YamlProject;
import org.catrobat.catroid.yaml.YamlSprite;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;

public class Translator {

	private static Translator instance;
	private XStream xstream;
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
	private ReentrantLock saveLoadLock = new ReentrantLock();
	private YamlConfig yamlConfig;

	private Translator() throws IOException {

		xstream = new XStream(new PureJavaReflectionProvider(
				new FieldDictionary(new CatroidFieldKeySorter())));
		xstream.processAnnotations(Project.class);
		xstream.processAnnotations(XmlHeader.class);
		xstream.processAnnotations(UserVariablesContainer.class);
		setXstreamAliases();

		yamlConfig = new YamlConfig();
		setYamlConfig();
	}

	public static String getXMLHeader() {
		return XML_HEADER;
	}

	private void setXstreamAliases() {
		xstream.alias("look", LookData.class);
		xstream.alias("sound", SoundInfo.class);
		xstream.alias("userVariable", UserVariable.class);

		xstream.alias("broadcastScript", BroadcastScript.class);
		xstream.alias("script", Script.class);
		xstream.alias("object", Sprite.class);
		xstream.alias("startScript", StartScript.class);
		xstream.alias("whenScript", WhenScript.class);

		xstream.aliasField("object", BrickBaseType.class, "sprite");

		xstream.alias("broadcastBrick", BroadcastBrick.class);
		xstream.alias("broadcastReceiverBrick", BroadcastReceiverBrick.class);
		xstream.alias("broadcastWaitBrick", BroadcastWaitBrick.class);
		xstream.alias("changeBrightnessByNBrick",
				ChangeBrightnessByNBrick.class);
		xstream.alias("changeGhostEffectByNBrick",
				ChangeGhostEffectByNBrick.class);
		xstream.alias("changeSizeByNBrick", ChangeSizeByNBrick.class);
		xstream.alias("changeVariableBrick", ChangeVariableBrick.class);
		xstream.alias("changeVolumeByNBrick", ChangeVolumeByNBrick.class);
		xstream.alias("changeXByNBrick", ChangeXByNBrick.class);
		xstream.alias("changeYByNBrick", ChangeYByNBrick.class);
		xstream.alias("clearGraphicEffectBrick", ClearGraphicEffectBrick.class);
		xstream.alias("comeToFrontBrick", ComeToFrontBrick.class);
		xstream.alias("foreverBrick", ForeverBrick.class);
		xstream.alias("glideToBrick", GlideToBrick.class);
		xstream.alias("goNStepsBackBrick", GoNStepsBackBrick.class);
		xstream.alias("hideBrick", HideBrick.class);
		xstream.alias("ifLogicBeginBrick", IfLogicBeginBrick.class);
		xstream.alias("ifLogicElseBrick", IfLogicElseBrick.class);
		xstream.alias("ifLogicEndBrick", IfLogicEndBrick.class);
		xstream.alias("ifOnEdgeBounceBrick", IfOnEdgeBounceBrick.class);
		xstream.alias("legoNxtMotorActionBrick", LegoNxtMotorActionBrick.class);
		xstream.alias("legoNxtMotorStopBrick", LegoNxtMotorStopBrick.class);
		xstream.alias("legoNxtMotorTurnAngleBrick",
				LegoNxtMotorTurnAngleBrick.class);
		xstream.alias("legoNxtPlayToneBrick", LegoNxtPlayToneBrick.class);
		xstream.alias("loopBeginBrick", LoopBeginBrick.class);
		xstream.alias("loopEndBrick", LoopEndBrick.class);
		xstream.alias("loopEndlessBrick", LoopEndlessBrick.class);
		xstream.alias("moveNStepsBrick", MoveNStepsBrick.class);
		xstream.alias("nextLookBrick", NextLookBrick.class);
		xstream.alias("noteBrick", NoteBrick.class);
		xstream.alias("placeAtBrick", PlaceAtBrick.class);
		xstream.alias("playSoundBrick", PlaySoundBrick.class);
		xstream.alias("pointInDirectionBrick", PointInDirectionBrick.class);
		xstream.alias("pointToBrick", PointToBrick.class);
		xstream.alias("repeatBrick", RepeatBrick.class);
		xstream.alias("setBrightnessBrick", SetBrightnessBrick.class);
		xstream.alias("setGhostEffectBrick", SetGhostEffectBrick.class);
		xstream.alias("setLookBrick", SetLookBrick.class);
		xstream.alias("setSizeToBrick", SetSizeToBrick.class);
		xstream.alias("setVariableBrick", SetVariableBrick.class);
		xstream.alias("setVolumeToBrick", SetVolumeToBrick.class);
		xstream.alias("setXBrick", SetXBrick.class);
		xstream.alias("setYBrick", SetYBrick.class);
		xstream.alias("showBrick", ShowBrick.class);
		xstream.alias("speakBrick", SpeakBrick.class);
		xstream.alias("stopAllSoundsBrick", StopAllSoundsBrick.class);
		xstream.alias("turnLeftBrick", TurnLeftBrick.class);
		xstream.alias("turnRightBrick", TurnRightBrick.class);
		xstream.alias("waitBrick", WaitBrick.class);
		xstream.alias("whenBrick", WhenBrick.class);
		xstream.alias("whenStartedBrick", WhenStartedBrick.class);
	}

	private void setYamlConfig() {
		yamlConfig.writeConfig.setEscapeUnicode(false);
		yamlConfig.writeConfig.setIndentSize(2);
		yamlConfig.setClassTag("program", YamlProject.class);
		yamlConfig.setPropertyDefaultType(YamlProject.class, "objects",
				TreeMap.class);
		yamlConfig.setPropertyElementType(YamlProject.class, "objects",
				YamlSprite.class);
		yamlConfig.setPropertyElementType(YamlSprite.class, "looks",
				LookData.class);
		yamlConfig.setPropertyElementType(YamlSprite.class, "sounds",
				SoundInfo.class);

	}

	public synchronized static Translator getInstance() {
		if (instance == null) {
			try {
				instance = new Translator();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	public Project loadProjectFromXML(File xmlProject) {
		saveLoadLock.lock();
		try {
			BufferedReader projectReader = new BufferedReader(new FileReader(xmlProject));
			Project returned = (Project) xstream.fromXML(projectReader);
			projectReader.close();
			return returned;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			saveLoadLock.unlock();
		}
	}

	public boolean saveProjectToXML(Project project, String destFolder) {
		saveLoadLock.lock();
		if (project == null) {
			saveLoadLock.unlock();
			return false;
		}
		try {
			String projectFile = xstream.toXML(project);
			File projectXML = new File(destFolder + "/code.xml");

			BufferedWriter writer = new BufferedWriter(new FileWriter(
					projectXML));
			writer.write(XML_HEADER.concat(projectFile));
			writer.flush();
			writer.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			saveLoadLock.unlock();
		}
	}

	public boolean saveProjectToXML(Project project) {
		saveLoadLock.lock();
		if (project == null) {
			saveLoadLock.unlock();
			return false;
		}
		try {
			String projectFile = xstream.toXML(project);
			File projectXML = new File("code.xml");

			BufferedWriter writer = new BufferedWriter(new FileWriter(
					projectXML));
			writer.write(XML_HEADER.concat(projectFile));
			writer.flush();
			writer.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			saveLoadLock.unlock();
		}
	}

	public String getXMLStringOfAProject(Project project) {
		return xstream.toXML(project);
	}

	public String getCatrobatLanguageStringOfAProject(Project project) {

		saveProjectToCatrobatLanguage(new YamlProject(project),
				System.getProperty("java.io.tmpdir"));
		BufferedReader input = null;
		try {
			input = new BufferedReader(new FileReader(new File(
					System.getProperty("java.io.tmpdir") + "/code.yml")));
			StringBuffer buf = new StringBuffer();
			while (input.ready()) {
				buf.append(input.readLine());
			}
			return buf.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

		return null;
	}

	public boolean saveProjectToCatrobatLanguage(YamlProject project,
			String destFolder) {
		saveLoadLock.lock();
		if (project == null) {
			saveLoadLock.unlock();
			return false;
		}
		try {
			File projectYAML = new File(destFolder + "/code.yml");

			YamlWriter yamlWriter = new YamlWriter(new FileWriter(projectYAML),
					yamlConfig);

			yamlWriter.write(project);
			yamlWriter.close();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			saveLoadLock.unlock();
		}
	}

	public boolean saveProjectToCatrobatLanguage(YamlProject project) {
		saveLoadLock.lock();
		if (project == null) {
			saveLoadLock.unlock();
			return false;
		}
		try {
			File projectYAML = new File("code.yml");

			YamlWriter yamlWriter = new YamlWriter(new FileWriter(projectYAML),
					yamlConfig);

			yamlWriter.write(project);
			yamlWriter.close();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			saveLoadLock.unlock();
		}
	}

	public YamlProject loadProjectFromCatrobatLanguage(File yamlProject) {
		saveLoadLock.lock();
		try {
			YamlReader yamlReader = new YamlReader(new FileReader(yamlProject),
					yamlConfig);

			YamlProject returned = (YamlProject) yamlReader.read();
			yamlReader.close();
			return returned;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			saveLoadLock.unlock();
		}
	}

	public void convertFromXMLToCatrobatLanguage(File xmlProject,
			String destFolder) {
		saveLoadLock.lock();
		try {
			Project project = loadProjectFromXML(xmlProject);
			saveProjectToCatrobatLanguage(new YamlProject(project), destFolder);
		} finally {
			saveLoadLock.unlock();
		}
	}

	public void convertFromXMLToCatrobatLanguage(File xmlProject) {
		saveLoadLock.lock();
		try {
			Project project = loadProjectFromXML(xmlProject);
			saveProjectToCatrobatLanguage(new YamlProject(project));
		} finally {
			saveLoadLock.unlock();
		}
	}

	public void convertFromCatrobatLanguageToXML(File catrobatFile,
			String destFolder) {
		saveLoadLock.lock();
		try {
			YamlProject catrobatProject = loadProjectFromCatrobatLanguage(catrobatFile);
			Project project = new Project(catrobatProject);
			saveProjectToXML(project, destFolder);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			saveLoadLock.unlock();
		}
	}

	public void convertFromCatrobatLanguageToXML(File catrobatFile) {
		saveLoadLock.lock();
		try {
			YamlProject catrobatProject = loadProjectFromCatrobatLanguage(catrobatFile);
			Project project = new Project(catrobatProject);
			saveProjectToXML(project);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			saveLoadLock.unlock();
		}
	}
}
