/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.utils.triplestore;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FileSplitter {

    //template for how dates will be converted in this class, note how matches the regexes //note Z is for timezone..
    final private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd'T'HH_mm_ssZ");
    //templates for generating output filenames based on position and integer
    final private static String addFilePattern = "add_{0}_{1}.{2}";
    final private static String subtractFilePattern = "subtract_{0}_{1}.{2}";
    final private static String fileNameRegExPattern = "^(add|subtract)_(\\d+)_(\\d{4}_\\d{2}_\\d{2}T\\d{2}_\\d{2}_\\d{2}(?:\\+|-)\\d{4})\\.";

    //600 to leave room for the extra data to come later (username, password, sparql update boilerplate and url encoding) and remain below 2MB (TOMCAT default limit) in posts
    final private int maxOutputFileSize;

    final private File fragmentDirectory;
    //the file extension to be used by this splitter
    final private String extension;
    //RegEx pattern for detecting files that match the generating templates ;
    final private Pattern fileNameRegEx;

    protected FileSplitter(File fragmentDirectory, String extension){
        //default size to about 1.2 meg
        this(fragmentDirectory, extension, 1024*600*2);
    }

    protected FileSplitter(File fragmentDirectory, String extension, int maxOutputFileSize){
        if (StringUtils.trimToNull(extension) == null) throw new IllegalArgumentException("extension must not be null or empty");
        if (fragmentDirectory == null) throw new NullArgumentException("fragmentDirectory");
        if (fragmentDirectory.exists()){
            if(!fragmentDirectory.isDirectory()) throw new IllegalArgumentException(MessageFormat.format("FragmentDirectory \"{0}\" must be a directory", fragmentDirectory));
        }
        else if(!fragmentDirectory.mkdirs())
            throw new IllegalArgumentException(MessageFormat.format("Could not create FragmentDirectory at \"{0}\"", fragmentDirectory));

        this.fragmentDirectory = fragmentDirectory;
        this.extension = extension;
        this.fileNameRegEx = Pattern.compile(fileNameRegExPattern + extension);
        //insist on a hard minimum of 64kb for fragment size
        int hardMinimum = 64*1024;
        this.maxOutputFileSize = maxOutputFileSize > hardMinimum ? maxOutputFileSize : hardMinimum;
    }

    private String getPattern(Type aType){
        switch(aType){
            case Additions: return addFilePattern;
            case Subtractions: return subtractFilePattern;
        }
        throw new NotImplementedException();
    }

    public void split(File fileToSplit, Date timeStamp, Type type) throws IOException{
        if (fileToSplit == null) throw new NullArgumentException("fileToSplit");
        if (timeStamp == null) throw new NullArgumentException("timeStamp");
        if (!fileToSplit.exists() || !fileToSplit.isFile())
            throw new IllegalArgumentException("fileToSplit must be a valid accessible file");

        //collection of files that have been created that should be cleared up if a split fails
        List<File> filesCreated = new ArrayList<File>();

        String fileNamePattern = getPattern(type);
        String dateTimeStamp = FileSplitter.dateFormat.format(timeStamp);
        try {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(fileToSplit), "UTF8"));
                String str;
                ByteArrayOutputStream mainOutputBuffer = new ByteArrayOutputStream(maxOutputFileSize);
                StringBuilder internalBuffer = new StringBuilder();

                int counter = 0;
                while ((str = in.readLine()) != null) {
                    String trimmedStr = StringUtils.trimToNull(str);
                    //if it has content add the current line to out internal buffer
                    if(trimmedStr != null) internalBuffer.append(trimmedStr);
                    //always add a new line for this line in the file.
                    internalBuffer.append("\n");

                    //if we have completed an internalBuffer fill we decide if we can flush it to the mainOutputBuffer.
                    if (isPotentialSplitPoint(str)) {
                        byte[] bufferBytes = internalBuffer.toString().getBytes("utf-8");
                        //if adding the current internal buffer would exceed our max we flush the main outputBuffer to a file and reset
                        if (mainOutputBuffer.size() + bufferBytes.length > maxOutputFileSize) {
                            File file = new File(fragmentDirectory, MessageFormat.format(fileNamePattern, Integer.toString(counter), dateTimeStamp, extension));
                            flushBufferToFile(mainOutputBuffer.toByteArray(), file);
                            filesCreated.add(file);
                            mainOutputBuffer = new ByteArrayOutputStream(maxOutputFileSize);
                            counter++;
                        }
                        IOUtils.copy(new ByteArrayInputStream(bufferBytes), mainOutputBuffer);
                        internalBuffer = new StringBuilder();
                    }
                }
                //output the remaining contents of the buffer
                byte[] remainder = mainOutputBuffer.toByteArray();
                if(remainder.length > 0) {
                    File file = new File(fragmentDirectory, MessageFormat.format(fileNamePattern, Integer.toString(counter), dateTimeStamp, extension));
                    flushBufferToFile(mainOutputBuffer.toByteArray(), file);
                    filesCreated.add(file);
                }
            } finally {
                if (in != null) in.close();
            }
        }
        catch (IOException e) {
            //TODO: report on where we are (add logging)?
            //try to clear out any files created during this run - which has now failed..
            for(File file : filesCreated){
                file.delete(); //allow delete to fail as it is safer - not a massive problem if we can't delete it at this point.
            }
            //rethrow e to bring the processing to a halt.
            throw e;
        }
    }

    //method to look at the line contents and decide if this is a split point.
    public abstract boolean isPotentialSplitPoint(String lineContent);

    private void flushBufferToFile(byte[] buffer, File outputFile) throws IOException{
        if(buffer == null) throw new NullArgumentException("buffer");
        if(outputFile == null) throw new NullArgumentException("outputFile");
        OutputStream outputStream = null;
        try {
            outputStream = (new BufferedOutputStream(new FileOutputStream(outputFile)));
            IOUtils.copy(new ByteArrayInputStream(buffer), outputStream);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    public Type getFileType(File file){
        if(file == null) throw new NullArgumentException("file");
        Matcher matcher = fileNameRegEx.matcher(file.getName());
        if (matcher.find()) {
            return Type.getType(matcher.group(1));
        }
        throw new IllegalStateException("invalid file passed to getFileType");
    }

    public List<File> getFragmentFilesInOrder(boolean subtractFirst){
        if (fragmentDirectory == null) throw new NullArgumentException("fragmentDirectory");
        if (!fragmentDirectory.exists() || !fragmentDirectory.isDirectory())
            throw new IllegalArgumentException("outputDirectory must be a valid accessible directory");

        //get all files that look like they were put into the fragment directory by this kind of splitter
        File [] files = fragmentDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                Matcher matcher = fileNameRegEx.matcher(name);
                return matcher.matches();
            }
        });
        if(files == null) files = new File[0];

        final boolean listSubtractFilesFirst = subtractFirst;

        List<File> filesToSort = Arrays.asList(files);
        Collections.sort(filesToSort, new Comparator<File>() {
            private MessageFormat errorMessageFormat = new MessageFormat("Error parsing sparql fragment files : {0}");
            @Override
            public int compare(File o1, File o2) {
                Type type1;
                Date date1;
                int index1;
                Type type2;
                Date date2;
                int index2;
                Matcher matcher1 = fileNameRegEx.matcher(o1.getName());
                Matcher matcher2 = fileNameRegEx.matcher(o2.getName());
                try {
                    if (matcher1.find()) {
                        type1 = Type.getType(matcher1.group(1));
                        index1 = Integer.parseInt(matcher1.group(2));
                        date1 = dateFormat.parse(matcher1.group(3));
                    } else
                        throw new IllegalStateException(errorMessageFormat.format(MessageFormat.format("filename {0} does not match pattern", o1.getName())));
                    if (matcher2.find()) {
                        type2 = Type.getType(matcher2.group(1));
                        index2 = Integer.parseInt(matcher2.group(2));
                        date2 = dateFormat.parse(matcher2.group(3));
                    } else
                        throw new IllegalStateException(errorMessageFormat.format(MessageFormat.format("filename {0} does not match pattern", o2.getName())));
                } catch (IllegalStateException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IllegalStateException("error parsing sparql fragment files : {0}", e);
                }
                if (date1.before(date2)) return -1;
                else if (date1.after(date2)) return 1;
                // if date alone is not enough to sort, move onto type
                else {
                    if (type1.getValue() < type2.getValue()) return listSubtractFilesFirst ? 1 : -1;
                    if (type1.getValue() > type2.getValue()) return listSubtractFilesFirst ? -1 : 1;
                    else {
                        if(index1 < index2) return -1;
                        if(index1 > index2) return 1;
                        else {
                            throw new IllegalStateException("Should never have equal files in sparql fragment directory");
                        }
                    }
                }
            }
        });

        return filesToSort;
    }

    public enum Type {
        Additions(0, "add"),
        Subtractions(1, "subtract");

        private final String name;

        public String getName() {
            return name;
        }

        private final int value;

        public int getValue() {
            return value;
        }

        Type(int value, String name) {
            if (StringUtils.trimToNull(name) == null) throw new NullArgumentException("name");
            this.name = name;
            this.value = value;
        }

        private static Type getType(String aString) {
            for (Type aType : Type.values()) {
                if (aString.equals(aType.getName())) return aType;
            }
            throw new NotImplementedException();
        }
    }

    public static class N3Splitter extends FileSplitter{
        //default of 122880
        public N3Splitter(File fragmentDirectory){super(fragmentDirectory, "n3");}
        public N3Splitter(File fragmentDirectory, int maxOutputFileSize){super(fragmentDirectory, "n3", maxOutputFileSize);}
        @Override
        public boolean isPotentialSplitPoint(String lineContent) {
            //only empty lines are potential split points for N3 content.
            return StringUtils.trimToNull(lineContent) == null;
        }
    }

    public static class NTriplesSplitter extends FileSplitter{
        public NTriplesSplitter(File fragmentDirectory){super(fragmentDirectory, "nt");}
        public NTriplesSplitter(File fragmentDirectory, int maxOutputFileSize){super(fragmentDirectory, "nt", maxOutputFileSize);}
        @Override
        public boolean isPotentialSplitPoint(String lineContent) {
            //every line in the file is a potential split point for NTriples.
            return true;
        }
    }
}
