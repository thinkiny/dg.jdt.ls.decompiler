/*******************************************************************************
 * Copyright (c) 2017 David Gileadi.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Gileadi - initial API and implementation
 *******************************************************************************/
package dg.jdt.ls.decompiler.fernflower;

import static org.eclipse.jdt.ls.core.internal.handlers.MapFlattener.getValue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.ls.core.internal.IContentProvider;
import org.eclipse.jdt.ls.core.internal.IDecompiler;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.struct.IDecompiledData;

public class FernflowerDecompiler implements IDecompiler {

	public static final String OPTIONS_KEY = "java.decompiler.fernflower";
	public static final String DECOMPILED_HEADER = " // Source code is unavailable, and was generated by the Fernflower decompiler.\n";

	public Map<String, Object> options;

	@Override
	public void setPreferences(Preferences preferences) {
		options = new HashMap<String, Object>();

		options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "1");
		options.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
		options.put(IFernflowerPreferences.ASCII_STRING_CHARACTERS, "1");

		Object overrides = getValue(preferences.asMap(), OPTIONS_KEY);
		if (overrides instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> overridesMap = (Map<String, Object>) overrides;
			for (String key : overridesMap.keySet()) {
				Object value = overridesMap.get(key);
				if (value instanceof Boolean) {
					value = ((Boolean) value).booleanValue() ? "1" : "0";
				}
				options.put(key, value.toString());
			}
		}
	}

	@Override
	public String getContent(URI uri, IProgressMonitor monitor) throws CoreException {
		return getContent(new BytecodeProvider(uri), monitor);
	}

	@Override
	public String getSource(IClassFile classFile, IProgressMonitor monitor) throws CoreException {
		return getContent(new BytecodeProvider(classFile), monitor);
	}

	private String getContent(BytecodeProvider provider, IProgressMonitor monitor) throws CoreException {
		ResultSaver saver = new ResultSaver();
		IFernflowerLogger logger = new DummyLogger();

		BaseDecompiler fernflower = new BaseDecompiler(provider, saver, options, logger);
		try {
			fernflower.addSpace(new File("Fake.class"), true);
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, "dg.jdt.ls.decompiler.fernflower",
					"Error adding fake class to decompile", e));
		}
		fernflower.decompileContext();

		return DECOMPILED_HEADER + saver.getContent();
	}
}
