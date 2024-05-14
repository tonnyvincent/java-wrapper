/*
 * FaceSDK Library Interface
 */
package Luxand;

import com.sun.jna.*;
import com.sun.jna.ptr.*;
import Luxand.FSDK.*;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class FSDKCam {
    
    // Types
    
    public static class HCamera {
        protected int hcamera;
    }
    
    public static class FSDK_VideoFormatInfo extends com.sun.jna.Structure {

        public int Width, Height, BPP;

        public static class ByValue extends FSDK_VideoFormatInfo implements com.sun.jna.Structure.ByValue {}
        public static class ByReference extends FSDK_VideoFormatInfo implements com.sun.jna.Structure.ByReference {}

        @Override
        public String toString(){
            return Integer.toString(Width).trim() + "x" + Integer.toString(Height).trim() + ", " + Integer.toString(BPP).trim() + " BPP";
        }

        protected java.util.List<String> getFieldOrder() {
            return Arrays.asList("Width", "Height", "BPP");
        }
    }

    public static class FSDK_VideoFormats {
        public FSDK_VideoFormatInfo.ByValue[] formats;
    }
    
    public static class TCameras {
        public String[] cameras;
    }

    // Library
    private static final NativeLibrary library = NativeLibrary.getInstance((Platform.isWindows() || Platform.isWindowsCE()) ? "facesdk" : "fsdk");

    // Functions

    private static native int FSDK_SetHTTPProxy(String ServerNameOrIPAddress, short Port, String UserName, String Password);
    private static native int FSDK_InitializeCapturing();
    private static native int FSDK_FinalizeCapturing();
    private static native int FSDK_GetCameraList(PointerByReference CameraList, IntByReference CameraCount);
    private static native int FSDK_GetCameraListEx(PointerByReference CameraNameList, PointerByReference CameraDevicePathList, IntByReference CameraCount);
    private static native int FSDK_FreeCameraList(Pointer CameraList, int CameraCount);
    private static native int FSDK_OpenIPVideoCamera(int CompressionType, String URL, String Username, String Password, int TimeoutSeconds, IntByReference CameraHandle);
    private static native int FSDK_OpenVideoCamera(byte[] CameraName, IntByReference CameraHandle);
    private static native int FSDK_CloseVideoCamera(int CameraHandle);
    private static native int FSDK_GrabFrame(int CameraHandle, IntByReference Image);
    private static native int FSDK_SetCameraNaming(byte UseDevicePathAsName);
    private static native int FSDK_GetVideoFormatList(byte[] CameraName, PointerByReference VideoFormatList, IntByReference VideoFormatCount);
    private static native int FSDK_FreeVideoFormatList(Pointer VideoFormatList);
    private static native int FSDK_SetVideoFormat(byte[] CameraName, FSDK_VideoFormatInfo.ByValue VideoFormat);

    // Variables
    private static final Pointer FSDK_VIDEO_FORMAT_SIZE = library.getGlobalVariableAddress("FSDK_VIDEO_FORMAT_SIZE");
    private static final Pointer FSDK_VideoFormatDescription = library.getGlobalVariableAddress("FSDK_VideoFormatDescription");

    public static final int VIDEO_FORMAT_SIZE;
    public static final String[] VideoFormatDescription;

    static {
        Native.register(library);
        VIDEO_FORMAT_SIZE = FSDK_VIDEO_FORMAT_SIZE.getInt(0);
        VideoFormatDescription = CameraUtil.getStringArray(FSDK_VideoFormatDescription, VIDEO_FORMAT_SIZE);
    }
    
    // Public interface
    
    public static int SetHTTPProxy(String ServerNameOrIPAddress, int Port, String UserName, String Password){
        if (Port < 0 || Port > 65535)
            return FSDK.FSDKE_INVALID_ARGUMENT;
        return FSDK_SetHTTPProxy(ServerNameOrIPAddress, (short)Port, UserName, Password);
    }
    
    public static int InitializeCapturing(){
        return FSDK_InitializeCapturing();
    }

    public static int FinalizeCapturing(){
        return FSDK_FinalizeCapturing();
    }

    public static int GetCameraList(TCameras CameraList, int[] CameraCount){
        if (CameraUtil.isEnabled()){
            if (CameraCount.length < 1)
                return FSDK.FSDKE_INVALID_ARGUMENT;

            PointerByReference ptmp = new PointerByReference();
            IntByReference tmp = new IntByReference();
            int res = FSDK_GetCameraList(ptmp, tmp);
            if (res == FSDK.FSDKE_OK){
                int cnt = tmp.getValue();
                Pointer p = ptmp.getValue();
                CameraList.cameras = CameraUtil.getPlatformStringArray(p, cnt);
                CameraCount[0] = cnt;
                FSDK_FreeCameraList(p, cnt);
            }
            
            return res;
        } else {
            return FSDK.FSDKE_FAILED;
        }
    }
    
    public static int GetCameraListEx(TCameras CameraNameList, TCameras CameraDevicePathList, int[] CameraCount){
        if (CameraUtil.isEnabled()){
            if (CameraCount.length < 1)
                return FSDK.FSDKE_INVALID_ARGUMENT;
        
            PointerByReference ptmp = new PointerByReference();
            PointerByReference ptmp2 = new PointerByReference();
            IntByReference tmp = new IntByReference();
            int res = FSDK_GetCameraListEx(ptmp, ptmp2, tmp);
            if (res == FSDK.FSDKE_OK){
                int cnt = tmp.getValue();
                Pointer p = ptmp.getValue();
                Pointer p2 = ptmp2.getValue();
                CameraNameList.cameras = CameraUtil.getPlatformStringArray(p, cnt);
                CameraDevicePathList.cameras = CameraUtil.getPlatformStringArray(p2, cnt);
                CameraCount[0] = cnt;
                FSDK_FreeCameraList(p, cnt);
                FSDK_FreeCameraList(p2, cnt);
            }
            return res;
        } else {
            return FSDK.FSDKE_FAILED;
        }
    }
    
    
    public static int OpenIPVideoCamera(int CompressionType, String URL, String Username, String Password, int TimeoutSeconds, HCamera CameraHandle){
        IntByReference tmp = new IntByReference();
        int res = FSDK_OpenIPVideoCamera(CompressionType, URL, Username, Password, TimeoutSeconds, tmp);
        CameraHandle.hcamera = tmp.getValue();
        return res;
    }

    public static int CloseVideoCamera(HCamera CameraHandle){
        return FSDK_CloseVideoCamera(CameraHandle.hcamera);
    }

    public static int GrabFrame(HCamera CameraHandle, HImage Image){
        IntByReference tmp = new IntByReference();
        int res = FSDK_GrabFrame(CameraHandle.hcamera, tmp);
        Image.himage = tmp.getValue();
        return res;
    }
    
    public static int OpenVideoCamera (String CameraName, HCamera CameraHandle) {
        if (CameraUtil.isEnabled()){
            IntByReference tmp = new IntByReference();
            int res = FSDK_OpenVideoCamera(CameraUtil.getBytes(CameraName), tmp);
            CameraHandle.hcamera = tmp.getValue();
            return res;
        } else {
            return FSDK.FSDKE_FAILED;
        }
        
    }
    public static int SetCameraNaming (boolean UseDevicePathAsName){
        if (CameraUtil.isEnabled()){
            byte bUseDevicePathAsName = (byte)(UseDevicePathAsName ? 1 : 0);
            return FSDK_SetCameraNaming(bUseDevicePathAsName);
        } else {
            return FSDK.FSDKE_FAILED;
        }
    }
    public static int GetVideoFormatList(String CameraName, FSDK_VideoFormats VideoFormatList, int[] VideoFormatCount){
        if (CameraUtil.isEnabled()){
            if (VideoFormatCount.length < 1)
                return FSDK.FSDKE_INVALID_ARGUMENT;
            PointerByReference ptmp = new PointerByReference();
            IntByReference count = new IntByReference();
            int res =  FSDK_GetVideoFormatList(CameraUtil.getBytes(CameraName), ptmp, count);
            if (res == FSDK.FSDKE_OK){
                int cnt = count.getValue();
                VideoFormatList.formats = new FSDK_VideoFormatInfo.ByValue[cnt];
                Pointer pt = ptmp.getValue();
                for (int i=0; i<cnt; i++){
                    VideoFormatList.formats[i] = new FSDK_VideoFormatInfo.ByValue();
                    VideoFormatList.formats[i].Width = pt.getInt(i * 12 + 0);
                    VideoFormatList.formats[i].Height = pt.getInt(i * 12 + 4);
                    VideoFormatList.formats[i].BPP = pt.getInt(i * 12 + 8);
                }
                VideoFormatCount[0] = cnt;
                FSDK_FreeVideoFormatList(pt);
            }
            return res;
        } else {
            return FSDK.FSDKE_FAILED;
        }
    }
    public static int SetVideoFormat(String CameraName, FSDK_VideoFormatInfo.ByValue VideoFormat){
        if (CameraUtil.isEnabled()){
            return FSDK_SetVideoFormat(CameraUtil.getBytes(CameraName), VideoFormat);
        } else {
            return FSDK.FSDKE_FAILED;
        }
    }

    private static class CameraUtil {

        private interface GetWideStringArrayFunction {
            String[] call(final Pointer pointer, final long offset, final int length);
        }

        private static Boolean enabled = null;
        private static String encoding = null;
        private static final GetWideStringArrayFunction getWideStringArrayFunction;

        static {
            GetWideStringArrayFunction wideStringMethod;
            final Package pkg = Native.class.getPackage();
            final int version = Integer.parseInt(pkg.getSpecificationVersion().substring(0, 1));
            try {
                if (version < 5) {
                    final Method method = Pointer.class.getMethod("getStringArray", long.class, int.class, boolean.class);
                    wideStringMethod = new GetWideStringArrayFunction() {
                        @Override
                        public String[] call(final Pointer pointer, final long offset, final int length) {
                            try {
                                return (String[])method.invoke(pointer, offset, length, true);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                            return new String[1];
                        }
                    };
                } else {
                    final Method method = Pointer.class.getMethod("getWideStringArray", long.class, int.class);
                    wideStringMethod = new GetWideStringArrayFunction() {
                        @Override
                        public String[] call(final Pointer pointer, final long offset, final int length) {
                            try {
                                return (String[])method.invoke(pointer, offset, length);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                            return new String[1];
                        }
                    };
                }
            } catch (NoSuchMethodException e) {
                wideStringMethod = null;
                e.printStackTrace();
                System.exit(-1);
            }

            getWideStringArrayFunction = wideStringMethod;
        }

        public static boolean isEnabled() {
            if (enabled != null)
                return enabled;
            return enabled = Platform.isWindows() || Platform.isWindowsCE() || Platform.isLinux();
        }

        public static String getEncoding() {
            if (encoding != null)
                return encoding;
            return encoding = Platform.isLinux() ? "UTF-8" : "UTF-16LE";
        }

        public static String[] getPlatformStringArray(Pointer pointer, int length) {
            return Platform.isLinux() ? getStringArray(pointer, length) : getWideStringArray(pointer, length);
        }

        public static String[] getWideStringArray(Pointer pointer, int length) {
            return getWideStringArrayFunction.call(pointer, 0, length);
        }

        public static String[] getStringArray(Pointer pointer, int length) {
            return pointer.getStringArray(0, length);
        }

        private static byte[] getBytes(String value) {
            try {
                byte[] bytes = value.getBytes(getEncoding());
                byte[] result = new byte[bytes.length + (Platform.isLinux() ? 1 : 2)];
                System.arraycopy(bytes, 0, result, 0, bytes.length);
                return result;
            } catch (UnsupportedEncodingException e) {
                return new byte[1];
            }
        }
    }
    
}
