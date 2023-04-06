import java.util.HashMap;
import java.util.List;

public final class Utils {
    public static void printListOfMaps(List<HashMap<String, HashMap<String, Double>>> list){
        for (HashMap<String, HashMap<String, Double>> map : list) {
            System.out.println(map.toString());
        }
    }
}
