package com.jamal2367.styx.proxy

import android.content.Context
import android.util.Log
import java.io.*
import java.net.URLEncoder
import java.util.*

object TorServiceUtils {
    private const val TAG = "TorUtils"

    // various console cmds
    const val SHELL_CMD_CHMOD = "chmod"
    const val SHELL_CMD_RM = "rm"
    private const val SHELL_CMD_PS = "ps"
    private const val SHELL_CMD_PIDOF = "pidof"
    const val CHMOD_EXE_VALUE = "700"

    // Check for 'su' binary
    val isRootPossible: Boolean
        get() {
            val log = StringBuilder()
            try {

                // Check if Superuser.apk exists
                var fileSU = File("/system/app/Superuser.apk")
                if (fileSU.exists()) return true
                fileSU = File("/system/app/superuser.apk")
                if (fileSU.exists()) return true
                fileSU = File("/system/bin/su")
                if (fileSU.exists()) {
                    val cmd = arrayOf(
                        "su"
                    )
                    val exitCode = doShellCommand(cmd, log, runAsRoot = false, waitFor = true)
                    return exitCode == 0
                }

                // Check for 'su' binary
                val cmd = arrayOf(
                    "which su"
                )
                val exitCode = doShellCommand(cmd, log, runAsRoot = false, waitFor = true)
                if (exitCode == 0) {
                    Log.d(TAG, "root exists, but not sure about permissions")
                    return true
                }
            } catch (e: IOException) {
                // this means that there is no root to be had (normally) so we won't
                // log anything
                Log.e(TAG, "Error checking for root access", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for root access", e)
                // this means that there is no root to be had (normally)
            }
            Log.e(TAG, "Could not acquire root permissions")
            return false
        }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
    fun findProcessId(context: Context): Int {
        val dataPath = context.filesDir.parentFile.parentFile.absolutePath
        val command = dataPath + "/" + OrbotHelper.ORBOT_PACKAGE_NAME + "/app_bin/tor"
        var procId = -1
        try {
            procId = findProcessIdWithPidOf(command)
            if (procId == -1) procId = findProcessIdWithPS(command)
        } catch (e: Exception) {
            try {
                procId = findProcessIdWithPS(command)
            } catch (e2: Exception) {
                Log.e(TAG, "Unable to get proc id for command: " + URLEncoder.encode(command), e2)
            }
        }
        return procId
    }

    // use 'pidof' command
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Throws(Exception::class)
    fun findProcessIdWithPidOf(command: String?): Int {
        var procId = -1
        val r = Runtime.getRuntime()
        val procPs: Process?
        val baseName = File(command).name
        // fix contributed my mikos on 2010.12.10
        procPs = r.exec(
            arrayOf(
                SHELL_CMD_PIDOF, baseName
            )
        )
        // procPs = r.exec(SHELL_CMD_PIDOF);
        val reader = BufferedReader(InputStreamReader(procPs.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            try {
                // this line should just be the process id
                procId = line!!.trim { it <= ' ' }.toInt()
                break
            } catch (e: NumberFormatException) {
                Log.e("TorServiceUtils", "unable to parse process pid: $line", e)
            }
        }
        return procId
    }

    // use 'ps' command
    @Throws(Exception::class)
    fun findProcessIdWithPS(command: String): Int {
        var procId = -1
        val r = Runtime.getRuntime()
        val procPs: Process? = r.exec(SHELL_CMD_PS)
        val reader = BufferedReader(InputStreamReader(procPs?.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line!!.indexOf(" $command") != -1) {
                val st = StringTokenizer(line, " ")
                st.nextToken() // proc owner
                procId = st.nextToken().trim { it <= ' ' }.toInt()
                break
            }
        }
        return procId
    }

    @Throws(Exception::class)
    fun doShellCommand(
        cmds: Array<String>, log: StringBuilder?, runAsRoot: Boolean,
        waitFor: Boolean
    ): Int {
        var exitCode = -1
        val proc: Process? = if (runAsRoot) Runtime.getRuntime().exec("su") else Runtime.getRuntime()
                .exec("sh")
        val out = OutputStreamWriter(proc?.outputStream)
        for (i in cmds.indices) {
            // TorService.logMessage("executing shell cmd: " + cmds[i] +
            // "; runAsRoot=" + runAsRoot + ";waitFor=" + waitFor);
            out.write(cmds[i])
            out.write("\n")
        }
        out.flush()
        out.write("exit\n")
        out.flush()
        if (waitFor) {
            val buf = CharArray(10)

            // Consume the "stdout"
            var reader = InputStreamReader(proc?.inputStream)
            var read: Int
            while (reader.read(buf).also { read = it } != -1) {
                log?.append(buf, 0, read)
            }

            // Consume the "stderr"
            reader = InputStreamReader(proc?.errorStream)
            read = 0
            while (reader.read(buf).also { read = it } != -1) {
                log?.append(buf, 0, read)
            }
            exitCode = proc!!.waitFor()
        }
        return exitCode
    }
}