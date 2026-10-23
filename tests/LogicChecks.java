package id.adhit.drivelite;
import java.util.*;
public final class LogicChecks {
  static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); System.out.println("PASS "+message); }
  public static void main(String[] args) {
    List<String> packages=new ArrayList<>(); for(int i=0;i<25;i++) packages.add("app."+i);
    Map<String,Integer> seed=new LinkedHashMap<>(); seed.put("app.0",0); seed.put("app.1",1);
    Map<String,Integer> first=CoverAssignments.assign(packages,new HashMap<>(),seed,25);
    check(new HashSet<>(first.values()).size()==25,"25 applications receive 25 distinct covers");
    List<String> reverse=new ArrayList<>(packages); Collections.reverse(reverse);
    check(first.equals(CoverAssignments.assign(reverse,first,seed,25)),"App sorting does not change saved cover assignments");
    List<String> changed=new ArrayList<>(packages); changed.remove("app.12"); changed.add("app.new");
    Map<String,Integer> next=CoverAssignments.assign(changed,first,seed,25);
    boolean retained=true; for(String pkg:packages) if(!pkg.equals("app.12")) retained &= first.get(pkg).equals(next.get(pkg));
    check(retained,"Replacing one app preserves all other cover assignments");
    check(next.get("app.new").equals(first.get("app.12")),"New app reuses the newly freed cover");
    Map<String,Integer> corrupt=new HashMap<>(); corrupt.put("app.0",99); corrupt.put("app.1",1); corrupt.put("app.2",1);
    check(new HashSet<>(CoverAssignments.assign(packages,corrupt,seed,25).values()).size()==25,"Invalid or duplicate saved assignments are repaired");
    packages.add("app.26"); check(CoverAssignments.assign(packages,first,seed,25).size()==26,"More than 25 apps still launch with bounded cover reuse");
    for(int kmh:new int[]{0,1,2}) check(SpeedMonitor.displayedKmh(kmh/3.6f)==0,"GPS drift "+kmh+" km/h stays stopped");
    for(int kmh:new int[]{3,4,5,30,100}) check(SpeedMonitor.displayedKmh(kmh/3.6f)==kmh,"Genuine speed "+kmh+" km/h is retained");
  }
}