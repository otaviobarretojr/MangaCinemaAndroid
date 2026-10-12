import com.mangacinema.reader.SceneDetector;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.List;

public final class SceneDetectorTest {
    public static void main(String[] args) throws Exception {
        int w = 100, h = 900;
        int[] a = new int[w*h];
        for (int i=0; i<a.length; i++) a[i] = 0xFF142030;
        for (int y=300; y<350; y++) for (int x=0; x<w; x++) a[y*w+x] = 0xFFFFFFFF;
        List<SceneDetector.Scene> cases = SceneDetector.detect(3, 720, 6480, w, h, a);
        if (cases.size() < 2) throw new AssertionError("Missed horizontal gutter: " + cases.size());
        for (SceneDetector.Scene sc: cases) {
            if (sc.endY <= sc.startY || sc.endY - sc.startY > 720*1.61)
                throw new AssertionError("Invalid crop: " + sc);
        }
        System.out.println("Synthetic page: OK - " + cases.size() + " scenes");
        if (args.length > 0) {
            BufferedImage im = ImageIO.read(new File(args[0]));
            int[] px = im.getRGB(0, 0, im.getWidth(), im.getHeight(), null, 0, im.getWidth());
            List<SceneDetector.Scene> real = SceneDetector.detect(1, 720, 7345, im.getWidth(), im.getHeight(), px);
            System.out.println("Real page 2: " + real.size() + " scene candidates");
            for (SceneDetector.Scene sc : real) System.out.println(sc);
        }
    }
}
