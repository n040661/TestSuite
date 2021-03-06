package Foo;

/**
 * Created by sagardisawal on 2/17/16.
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;


public class ZipFileRecordReader
        extends RecordReader<Text, BytesWritable>
{
    
    private FSDataInputStream fsin;

    private ZipInputStream zip;

    private Text currentKey;

  
    private BytesWritable currentValue;

  
    private boolean isFinished = false;

    /**
     * Initialise and open the ZIP file from the FileSystem
     */
    @Override
    public void initialize( InputSplit inputSplit, TaskAttemptContext taskAttemptContext )
            throws IOException, InterruptedException
    {
        FileSplit split = (FileSplit) inputSplit;
        Configuration conf = taskAttemptContext.getConfiguration();
        Path path = split.getPath();
        FileSystem fs = path.getFileSystem( conf );

  
        fsin = fs.open( path );
        zip = new ZipInputStream( fsin );
    }

 
    @Override
    public boolean nextKeyValue()
            throws IOException, InterruptedException
    {
        ZipEntry entry = null;
        try
        {
            entry = zip.getNextEntry();
        }
        catch ( ZipException e )
        {
            if ( ZipFileInputFormat.getLenient() == false )
                throw e;
        }

   
        if ( entry == null )
        {
            isFinished = true;
            return false;
        }

        currentKey = new Text( entry.getName() );


        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] temp = new byte[8192];
        while ( true )
        {
            int bytesRead = 0;
            try
            {
                bytesRead = zip.read( temp, 0, 8192 );
            }
            catch ( EOFException e )
            {
                if ( ZipFileInputFormat.getLenient() == false )
                    throw e;
                return false;
            }
            if ( bytesRead > 0 )
                bos.write( temp, 0, bytesRead );
            else
                break;
        }
        zip.closeEntry();

        // Uncompressed contents
        currentValue = new BytesWritable( bos.toByteArray() );
        return true;
    }

    
    @Override
    public void close()
            throws IOException
    {
        try { zip.close(); } catch ( Exception ignore ) { }
        try { fsin.close(); } catch ( Exception ignore ) { }
    }
}
