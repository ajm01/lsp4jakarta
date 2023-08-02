/*******************************************************************************
* Copyright (c) 2020, 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package org.eclipse.lsp4jakarta.lsp4e;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4jakarta.ls.api.JakartaLanguageClientAPI;
import org.eclipse.lsp4jakarta.commons.JakartaClasspathParams;
import org.eclipse.lsp4jakarta.commons.JakartaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCompletionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCompletionResult;
import org.eclipse.lsp4jakarta.commons.JakartaJavaFileInfo;
import org.eclipse.lsp4jakarta.commons.JakartaJavaFileInfoParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaProjectLabelsParams;
import org.eclipse.lsp4jakarta.commons.JavaCursorContextKind;
import org.eclipse.lsp4jakarta.commons.JavaCursorContextResult;
import org.eclipse.lsp4jakarta.commons.ProjectLabelInfoEntry;
import org.eclipse.lsp4jakarta.jdt.core.JDTServicesManager;
import org.eclipse.lsp4jakarta.jdt.core.JDTUtils;
import org.eclipse.lsp4jakarta.jdt.core.ProjectLabelManager;
import org.eclipse.lsp4jakarta.jdt.internal.core.ls.JDTUtilsLSImpl;
import org.eclipse.lsp4jakarta.jdt.core.PropertiesManagerForJava;

public class JakartaLanguageClient extends LanguageClientImpl implements JakartaLanguageClientAPI {

    public JakartaLanguageClient() {
        // do nothing
    }

    private IProgressMonitor getProgressMonitor(CancelChecker cancelChecker) {
        IProgressMonitor monitor = new NullProgressMonitor() {
            public boolean isCanceled() {
                cancelChecker.checkCanceled();
                return false;
            };
        };
        return monitor;
    }

    @Override
    public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(
            JakartaDiagnosticsParams jakartaParams) {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);

            List<PublishDiagnosticsParams> publishDiagnostics = new ArrayList<PublishDiagnosticsParams>();
            publishDiagnostics = JDTServicesManager.getInstance().getJavaDiagnostics(jakartaParams.getUris(), monitor);
            return publishDiagnostics;
        });
    }
    
    @Override
    public CompletableFuture<JakartaJavaCompletionResult> getJavaCompletion(JakartaJavaCompletionParams javaParams) {
        return CompletableFutures.computeAsync(cancelChecker -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
                CompletionList completionList;
				try {
					completionList = PropertiesManagerForJava.getInstance().completion(javaParams, JDTUtilsLSImpl.getInstance(), monitor);
					JavaCursorContextResult javaCursorContext = PropertiesManagerForJava.getInstance().javaCursorContext(javaParams, JDTUtilsLSImpl.getInstance(), monitor);               
	                return new JakartaJavaCompletionResult(completionList, javaCursorContext); 
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
        });
    }

    @Override
    public CompletableFuture<List<String>> getContextBasedFilter(JakartaClasspathParams classpathParams) {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            return JDTServicesManager.getInstance().getExistingContextsFromClassPath(classpathParams.getUri(),
                    classpathParams.getSnippetCtx());
        });
    }

    @Override
    public CompletableFuture<JavaCursorContextResult> getJavaCursorContext(JakartaJavaCompletionParams params) {
        JDTUtils utils = new JDTUtils();
        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            try {
                return JDTServicesManager.getInstance().javaCursorContext(params, utils, monitor);
            } catch (JavaModelException e) {
                return new JavaCursorContextResult(JavaCursorContextKind.IN_EMPTY_FILE, "");
            }
        });
    }

    public CompletableFuture<List<CodeAction>> getCodeAction(JakartaJavaCodeActionParams params) {
        JDTUtils utils = new JDTUtils();

        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            try {
                return (List<CodeAction>) JDTServicesManager.getInstance().getCodeAction(params, utils, monitor);
            } catch (JavaModelException e) {
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<List<ProjectLabelInfoEntry>> getAllJavaProjectLabels() {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            return ProjectLabelManager.getInstance().getProjectLabelInfo();
        });
    }
    
    @Override
    public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectLabels(JakartaJavaProjectLabelsParams javaParams) {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            return ProjectLabelManager.getInstance().getProjectLabelInfo(javaParams, JDTUtilsLSImpl.getInstance(), monitor);
        });
    }

    @Override
    public CompletableFuture<JakartaJavaFileInfo> getJavaFileInfo(JakartaJavaFileInfoParams javaParams) {
        return CompletableFutures.computeAsync(cancelChecker -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            return PropertiesManagerForJava.getInstance().fileInfo(javaParams, JDTUtilsLSImpl.getInstance(), monitor);
        });
    }
}
