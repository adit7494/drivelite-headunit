package id.adhit.drivelite;

import java.util.*;

/** Keeps existing applications' art stable and allocates unused slots before any repeats. */
final class CoverAssignments {
    static Map<String,Integer> assign(List<String> packages, Map<String,Integer> saved, Map<String,Integer> preferred, int count) {
        if(count<1) throw new IllegalArgumentException("No covers");
        Map<String,Integer> result=new LinkedHashMap<>();
        boolean[] used=new boolean[count];
        reserve(packages,saved,result,used);
        reserve(packages,preferred,result,used);
        for(String pkg:packages) {
            if(result.containsKey(pkg)) continue;
            int slot=0;
            while(slot<count && used[slot]) slot++;
            if(slot==count) slot=(pkg.hashCode() & 0x7fffffff)%count;
            result.put(pkg,slot); used[slot]=true;
        }
        return result;
    }
    private static void reserve(List<String> packages, Map<String,Integer> source, Map<String,Integer> result, boolean[] used) {
        for(String pkg:packages) {
            Integer slot=source.get(pkg);
            if(!result.containsKey(pkg) && slot!=null && slot>=0 && slot<used.length && !used[slot]) {
                result.put(pkg,slot); used[slot]=true;
            }
        }
    }
}
