import com.mangacinema.reader.SceneDetector;
import com.mangacinema.reader.SceneTiming;
import com.mangacinema.reader.YuvConverter;
import java.util.List;

public final class ExportTimingTest {
    public static void main(String[] args) {
        int w = 480, h = 848;
        int[] rgba = new int[w * h];
        for (int i = 0; i < rgba.length; i++) rgba[i] = (i & 1) == 0 ? 0xFFFFFFFF : 0xFF000000;
        byte[] i420 = new byte[w*h*3/2];
        byte[] nv12 = new byte[w*h*3/2];
        YuvConverter.rgbToYuv420(rgba,i420,w,h,false);
        YuvConverter.rgbToYuv420(rgba,nv12,w,h,true);
        if ((i420[0]&255) != 235 || (i420[1]&255) != 16)
            throw new AssertionError("Luma unexpected");
        if ((i420[w*h] & 255) != 128 || (nv12[w*h+1]&255)!=128)
            throw new AssertionError("Chroma unexpected");
        int[] a = new int[100*900];
        for(int i=0;i<a.length;i++) a[i]=0xff203040;
        for(int y=300;y<350;y++)for(int x=0;x<100;x++) a[y*100+x]=0xffffffff;
        List<SceneDetector.Scene> sc = SceneDetector.detect(1,720,6480,100,900,a);
        if(sc.size()<2) throw new AssertionError("Need 2+ segments");
        for(SceneDetector.Scene s:sc) {
            long duration=SceneTiming.motionMillis(s,1.0f);
            int start=SceneTiming.frames(duration,12,false);
            int following=SceneTiming.frames(duration,12,true);
            if(start<20 || following<=start) throw new AssertionError("Bad timing");
        }
        boolean rejected=false;
        try{ YuvConverter.rgbToYuv420(rgba,nv12,481,848,true); }
        catch(IllegalArgumentException expected){rejected=true;}
        if(!rejected) throw new AssertionError("Odd geometry should be rejected");
        System.out.println("Export timing and YUV420: OK; synthetic scenes="+sc.size());
    }
}
