import org.junit.jupiter.api.Test;
import utility.ArrayUtility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArrayUtilityTest {
    //Kiểm tra kết quả chính xác (Normal)
    @Test
    void sumArrayNormalTest(){
        assertEquals(500500, ArrayUtility.sumArray());
    }
    //Kiểm tra giá trị biên
    @Test
    public void sumArrayBoundaryUpperTest(){
        long expected = 1000L *(1000 +1)/2;
        assertEquals(expected, ArrayUtility.sumArray());
    }
    //Kiểm tra theo công thức GAUSS
    @Test
    public void sumArrayMatchesFormulaTest() {
        int n = 1000;
        long expected = (long) n * (n + 1) / 2;
        assertEquals(expected, ArrayUtility.sumArray());
    }
    //Kiểm tra tính ổn định của hàm
    @Test
    public void sumArrayConsistencyTest(){
        long result1 = ArrayUtility.sumArray();
        long result2 = ArrayUtility.sumArray();
        assertEquals(result1, result2);
    }
    //Kiểm tra giá trị không bị tràn
    @Test
    public void sumArrayDoestNotOverflowTest(){
        long result1 = ArrayUtility.sumArray();
        assertTrue(result1 < Long.MAX_VALUE,
                "Giá trị không vượt quá phạm vi của kiểu Long");
    }
}
