package com.ricardo.notasvozlocal;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.vosk.Model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

final class ModelRepository {
    static final String TAG = "NOTAS_VOZ";
    private static final String ASSET_MODEL_DIR = "model-es";
    private static final String INTERNAL_MODEL_DIR = "vosk-model-es";
    private static final String MODEL_VERSION = "3";
    private static final Object LOCK = new Object();
    private static Model cachedModel;
    private static final String[] REQUIRED = {
            "am/final.mdl",
            "conf/mfcc.conf",
            "conf/model.conf",
            "graph/Gr.fst",
            "graph/HCLr.fst",
            "graph/words.txt"
    };

    private ModelRepository() {
    }

    static Model load(Context context) throws Exception {
        synchronized (LOCK) {
            long started = System.currentTimeMillis();
            if (cachedModel != null) {
                Log.i(TAG, "Model reutilizado desde memoria. Duración carga ms=" + (System.currentTimeMillis() - started));
                logMemory("memoria tras reutilizar Model");
                return cachedModel;
            }
            File dir = new File(context.getFilesDir(), INTERNAL_MODEL_DIR);
            Log.i(TAG, "Ruta del modelo: " + dir.getAbsolutePath());
            long checkStarted = System.currentTimeMillis();
            if (!isInstalled(dir)) {
                Log.i(TAG, "Comprobación del modelo ms=" + (System.currentTimeMillis() - checkStarted));
                Log.i(TAG, "Inicio de copia del modelo local");
                deleteRecursively(dir);
                if (!dir.mkdirs() && !dir.isDirectory()) {
                    throw new IllegalStateException("No se pudo crear " + dir.getAbsolutePath());
                }
                copyAssetDir(context.getAssets(), ASSET_MODEL_DIR, dir);
                writeVersion(dir);
                verifyRequired(dir);
                verifyGraph(dir);
                Log.i(TAG, "Fin de copia del modelo local");
            } else {
                verifyRequired(dir);
                verifyGraph(dir);
                Log.i(TAG, "Modelo local ya instalado y verificado");
                Log.i(TAG, "Comprobación del modelo ms=" + (System.currentTimeMillis() - checkStarted));
            }
            long modelStarted = System.currentTimeMillis();
            cachedModel = new Model(dir.getAbsolutePath());
            Log.i(TAG, "Model creado sin excepción. Carga Model ms=" + (System.currentTimeMillis() - modelStarted));
            Log.i(TAG, "Duración total preparación modelo ms=" + (System.currentTimeMillis() - started));
            logMemory("memoria tras cargar Model");
            return cachedModel;
        }
    }

    private static boolean isInstalled(File dir) {
        File version = new File(dir, "model-version.txt");
        return dir.isDirectory()
                && version.isFile()
                && MODEL_VERSION.equals(readSmall(version))
                && hasRequired(dir)
                && hasGraph(dir);
    }

    private static boolean hasRequired(File dir) {
        for (String path : REQUIRED) {
            File file = new File(dir, path);
            Log.i(TAG, "Archivo obligatorio " + path + ": " + file.isFile());
            if (!file.isFile()) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasGraph(File dir) {
        boolean splitGraph = new File(dir, "graph/Gr.fst").isFile()
                && new File(dir, "graph/HCLr.fst").isFile();
        boolean fullGraph = new File(dir, "graph/HCLG.fst").isFile();
        Log.i(TAG, "Grafo split Gr.fst/HCLr.fst: " + splitGraph);
        Log.i(TAG, "Grafo completo HCLG.fst: " + fullGraph);
        return splitGraph || fullGraph;
    }

    static void logMemory(String label) {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        Log.i(TAG, label + " usedMB=" + (used / 1024 / 1024)
                + " totalMB=" + (runtime.totalMemory() / 1024 / 1024)
                + " maxMB=" + (runtime.maxMemory() / 1024 / 1024));
    }

    private static void verifyGraph(File dir) {
        if (!hasGraph(dir)) {
            throw new IllegalStateException("Falta grafo Vosk: se esperaba HCLG.fst o Gr.fst + HCLr.fst");
        }
    }

    private static void verifyRequired(File dir) {
        for (String path : REQUIRED) {
            File file = new File(dir, path);
            Log.i(TAG, "Archivo obligatorio encontrado: " + file.getAbsolutePath() + " -> " + file.isFile());
            if (!file.isFile()) {
                throw new IllegalStateException("Falta archivo obligatorio del modelo: " + path);
            }
        }
    }

    private static void copyAssetDir(AssetManager assets, String assetPath, File targetDir) throws Exception {
        String[] children = assets.list(assetPath);
        if (children == null || children.length == 0) {
            try (InputStream in = assets.open(assetPath);
                 FileOutputStream out = new FileOutputStream(targetDir)) {
                byte[] buffer = new byte[1024 * 256];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return;
        }
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IllegalStateException("No se pudo crear " + targetDir.getAbsolutePath());
        }
        for (String child : children) {
            copyAssetDir(assets, assetPath + "/" + child, new File(targetDir, child));
        }
    }

    private static void writeVersion(File dir) throws Exception {
        try (FileOutputStream out = new FileOutputStream(new File(dir, "model-version.txt"))) {
            out.write(MODEL_VERSION.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static String readSmall(File file) {
        try {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            return new String(data, java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static void deleteRecursively(File file) {
        if (!file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            Log.w(TAG, "No se pudo borrar " + file.getAbsolutePath());
        }
    }
}
