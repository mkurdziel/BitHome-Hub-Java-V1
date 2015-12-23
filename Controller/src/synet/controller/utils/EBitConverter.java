package synet.controller.utils;

public class EBitConverter
{
	public static int toUInt16(int[] p_data, int p_startIndex)
	{
		return 	(int)(0xff & p_data[p_startIndex]) << 8 | 
				(int)(0xff & p_data[p_startIndex + 1]); 
	}
	
	public static int toUInt32(int[] p_data, int p_startIndex)
	{
		return 	(int)(0xff & p_data[p_startIndex]) << 24 | 
				(int)(0xff & p_data[p_startIndex+1]) << 16 | 
				(int)(0xff & p_data[p_startIndex+2]) << 8 | 
				(int)(0xff & p_data[p_startIndex+3]); 
	}

	public static long toUInt64(int[] p_data, int p_startIndex)
	{
		return 	(long)(0xff & p_data[p_startIndex]) << 56 | 
				(long)(0xff & p_data[p_startIndex+1]) << 48 | 
				(long)(0xff & p_data[p_startIndex+2]) << 40 | 
				(long)(0xff & p_data[p_startIndex+3]) << 32 | 
				(long)(0xff & p_data[p_startIndex+4]) << 24 | 
				(long)(0xff & p_data[p_startIndex+5]) << 16 | 
				(long)(0xff & p_data[p_startIndex+6]) << 8 | 
				(long)(0xff & p_data[p_startIndex+7]); 
	}

	public static String toString(int[] p_data, int p_startIndex)
	{
		StringBuilder sb = new StringBuilder();
		int c;
		while( (c = p_data[p_startIndex++]) != 00)
		{
			sb.append((char)c);
		}
		
		return sb.toString();
	}
	
    /// <summary>
    ///   Extracts a value of a given width
    /// </summary>
    /// <param name="p_msgData"></param>
    /// <param name="p_nByteIdx"></param>
    /// <param name="p_nValueWidthInBytes"></param>
    /// <param name="p_nBuiltValue"></param>
    /// <returns></returns>
    public static int loadValueGivenWidth(
    		int[] p_data, 
    		int p_startIndex, 
    		int p_nValueWidthInBytes)
    {
    	int retVal = 0;

    	for (int nByteCt = 0; nByteCt < p_nValueWidthInBytes; nByteCt++)
    	{
    		retVal = (retVal << 8) + p_data[p_startIndex + nByteCt];
    	}

      return retVal;
    }
    
    public static byte[] longToBytes(long v) {
        byte[] writeBuffer = new byte[ 8 ];

        writeBuffer[0] = (byte)(v >>> 56);
        writeBuffer[1] = (byte)(v >>> 48);
        writeBuffer[2] = (byte)(v >>> 40);
        writeBuffer[3] = (byte)(v >>> 32);
        writeBuffer[4] = (byte)(v >>> 24);
        writeBuffer[5] = (byte)(v >>> 16);
        writeBuffer[6] = (byte)(v >>>  8);
        writeBuffer[7] = (byte)(v >>>  0);

        return writeBuffer;
    }

}
