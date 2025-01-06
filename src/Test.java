import java.util.Arrays;
import java.util.List;

public class Test {

	public static boolean containsAll(int[] list, List<Integer> array) {
		// TODO:  NOT EFFICIENT.  IF THE ITEMSETS ARE SORTED, IT WOULD BE MUCH FASTER!
		
        // For each element in the list, check if it exists in the array
        for (int element : array) {
        	boolean found = false;
        	
        	 for (int item : list) {
                 if (item == element) {
                     found = true;
                     break;
                 }
             }
        	 if(found == false) {
        		 return false;
        	 }
        }
        return true;
    }
//	public static void main(String[] args) {
//		
//	}
//	
    // Example usage
    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(3, 2, 1, 7);
        int[] array = {5, 4, 3, 2, 1};

        System.out.println(containsAll(array, list)); // 输出: true
    }
}
