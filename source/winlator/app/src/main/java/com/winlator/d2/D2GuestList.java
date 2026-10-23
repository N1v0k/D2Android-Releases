package com.winlator.d2;

import com.winlator.core.ProcessHelper;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;










public final class D2GuestList {
    private D2GuestList() {}


    public static final class Result {
        public final List<ProcessHelper.PStat> processes;

        public final int onlyByRuntime;

        Result(List<ProcessHelper.PStat> processes, int onlyByRuntime) {
            this.processes = processes;
            this.onlyByRuntime = onlyByRuntime;
        }
    }

    public static Result find(File proc, File rootfs, int uid, int selfPid) {
        Set<Integer> guests = new HashSet<>();
        for (D2GuestProcs.Proc p : D2GuestProcs.find(proc, rootfs, uid, selfPid, null)) guests.add(p.pid);
        List<ProcessHelper.PStat> list = ProcessHelper.getProcesses(proc,
            p -> guests.contains(p.pid) || oldRule(p, selfPid));
        int only = 0;
        for (ProcessHelper.PStat p : list) {
            if (!guests.contains(p.pid)) continue;
            if (!oldRule(p, selfPid) || !p.guestProcess) only++;
            p.guestProcess = true;
        }
        return new Result(list, only);
    }


    private static boolean oldRule(ProcessHelper.PStat p, int selfPid) {
        return p.parentPID == selfPid || p.pid > selfPid;
    }
}
