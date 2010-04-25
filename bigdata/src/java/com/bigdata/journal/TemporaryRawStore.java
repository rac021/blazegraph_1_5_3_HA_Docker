/**

Copyright (C) SYSTAP, LLC 2006-2007.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Feb 15, 2007
 */

package com.bigdata.journal;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.bigdata.LRUNexus;
import com.bigdata.counters.CounterSet;
import com.bigdata.io.DirectBufferPool;
import com.bigdata.journal.ha.Quorum;
import com.bigdata.mdi.AbstractResourceMetadata;
import com.bigdata.mdi.IResourceMetadata;
import com.bigdata.rawstore.AbstractRawWormStore;
import com.bigdata.rawstore.IMRMW;
import com.bigdata.rawstore.WormAddressManager;
import com.bigdata.relation.locator.ILocatableResource;
import com.bigdata.util.ChecksumUtility;

/**
 * A non-restart-safe store for temporary data that buffers data in memory until
 * the write cache overflows (or is flushed to the disk) and then converts to a
 * disk-based store. The backing file (if any) is released when the temporary
 * store is {@link #close()}d.
 * 
 * @see BufferMode#Temporary
 * @see DiskOnlyStrategy
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TemporaryRawStore.java 2356 2010-01-28 17:36:13Z martyncutcher
 *          $
 */
public class TemporaryRawStore extends AbstractRawWormStore implements IMRMW {

    protected static final Logger log = Logger.getLogger(TemporaryRawStore.class);

//    protected static final boolean INFO = log.isInfoEnabled();
    
    /**
     * Note: various things must be synchronized on {@link #buf} in order to
     * serialize reads, writes, etc. This is because it is {@link #buf} on which
     * the {@link DiskOnlyStrategy} itself is synchronized. For this reason it
     * is also a good idea to never allow {@link #buf} to become
     * <code>null</code>, hence it is declared <code>final</code> here.
     */
    private final DiskOnlyStrategy buf;
    
//    /**
//     * When non-<code>null</code> this is a direct {@link ByteBuffer}
//     * allocated using the {@link DirectBufferPool} during
//     * {@link #overflowToDisk()} and handed off to the {@link DiskOnlyStrategy}
//     * for use as its write cache. When non-<code>null</code> this buffer is
//     * {@link DirectBufferPool#release(ByteBuffer)}ed back to the
//     * {@link DirectBufferPool} in {@link #finalize()} to avoid a native memory
//     * leak.
//     */
//    private ByteBuffer writeCache = null;
    
    /**
     * Store identifier.
     */
    private final UUID uuid = UUID.randomUUID();
    
    /**
     * Note: this timestamp is NOT generated by a centralized time server. 
     */
    private final long createTime;

    /**
     * Return an empty {@link File} created using the temporary file name
     * mechanism. The file name will begin with <code>bigdata</code> and end
     * with <code>.tmp</code>. The file is marked for eventual deletion.
     */
    static protected File getTempFile() {

        try {

            final File file = File.createTempFile("bigdata", ".tmp");

            file.deleteOnExit();

            return file;

        } catch (IOException ex) {

            throw new RuntimeException(ex);

        }

    }

    /**
     * Return an empty {@link File} created using the temporary file name
     * mechanism in the specified directory. The directory will be created if it
     * does not exist and the caller has sufficient permissions. The file name
     * will begin with <code>bigdata</code> and end with <code>.tmp</code>. The
     * file is marked for eventual deletion.
     * 
     * @param tmpDir
     *            The directory within which to create the temporary file.
     */
    static public File getTempFile(final File tmpDir) {

        try {

            if (!tmpDir.exists()) {

                if (!tmpDir.mkdirs()) {

                    /*
                     * The return code is ignored. The directory could have been
                     * concurrently created so there is no reason to look at the
                     * return code here. If we can't create the file below then
                     * we have a problem and it will get reported.
                     */
                    
                }

            }
            
            final File file = File.createTempFile("bigdata", ".tmp", tmpDir);
            
            file.deleteOnExit();
            
            return file;
            
        } catch (IOException ex) {
        
            throw new RuntimeException(ex);
            
        }
        
    }
    
    /**
     * The UUID of this {@link TemporaryRawStore}. This is reported as part of
     * {@link #getResourceMetadata()} and may also be used to ensure that
     * {@link ILocatableResource}s created on a {@link TemporaryStore} are
     * placed within a unique namespace.
     */
    final public UUID getUUID() {
        
        return uuid;
        
    }
    
    /**
     * Create a {@link TemporaryRawStore}.
     */
    public TemporaryRawStore() {

        this(WormAddressManager.SCALE_UP_OFFSET_BITS);
        
    }
    
    /**
     * Create a {@link TemporaryRawStore}.
     * 
     * @param offsetBits
     *            This determines the capacity of the store file and the maximum
     *            length of a record. The value is passed through to
     *            {@link WormAddressManager#WormAddressManager(int)}.
     */
    public TemporaryRawStore(final int offsetBits) {

        this(0L/* maximumExtent */, offsetBits, getTempFile());
        
    }
    
    /**
     * Create a {@link TemporaryRawStore} with the specified configuration.
     * 
     * @param maximumExtent
     *            The maximum extent allowed for the {@link TemporaryRawStore}
     *            -or- ZERO (0L) iff no limit should be imposed.
     * 
     * @param offsetBits
     *            This determines the capacity of the store file and the maximum
     *            length of a record. The value is passed through to
     *            {@link WormAddressManager#WormAddressManager(int)}.
     * 
     * @param file
     *            The name of the backing file. The file will be created on
     *            demand if it does not exist. It will be an error if the file
     *            exists and has a non-zero size at that time. The file will be
     *            registered with the JVM for for "delete on exit" and will be
     *            deleted regardless as soon as the store is
     *            {@link #close() closed}.
     */
    public TemporaryRawStore(final long maximumExtent, final int offsetBits,
            final File file) {
        
        super(offsetBits);
        
        if(log.isInfoEnabled()) {
            
            log.info("offsetBits=" + offsetBits + ", file=" + file
//            ,new RuntimeException()
            );
            
        }
        
        // Note: timestamp is NOT assigned by a centralized service!
        this.createTime = System.currentTimeMillis();
        
//        try {
//
//            /*
//             * Try and acquire a direct buffer to serve as the initial in-memory
//             * extent and the write cache.
//             */
//            // Note: the timeout here is not such a good idea.  It could
//            // be triggered by a GC pause with the resulting temp store 
//            // then lacking a write cache.
//            this.writeCache = DirectBufferPool.INSTANCE.acquire(2000L,
//                    TimeUnit.MILLISECONDS);
//            
//        } catch (InterruptedException e) {
//            
//            throw new RuntimeException(e);
//            
//        } catch (TimeoutException e) {
//            
//            throw new RuntimeException(e);
//            
//        }
        
        /*
         * Note: The initial on disk capacity is exactly the capacity of the
         * write cache.  This implies that the file will be extended 32M at
         * a time (the default when the initialExtent is less than 32M).  
         */
        final long initialExtent = DirectBufferPool.INSTANCE.getBufferCapacity();
        
        /*
         * Note: This is the overflow trigger point. Since this class does not
         * support overflow, this value is essentially ignored. However, it must
         * be GTE [initialExtent] or an exception will be thrown.
         */
        final long overflowExtent = initialExtent;

        final FileMetadata md = new FileMetadata(//
                file,//
                BufferMode.Temporary,//
                false,// useDirectBuffers (ignored for disk-based modes)
                initialExtent, // The initial on disk capacity.
                overflowExtent, // Note: same as [initialExtent] (overflow trigger) 
                true,// create (ignored for temporary files).
                true,// isEmptyFile (file is either empty or does not exist)
                true, // deleteOnExit
                false, // readOnly
                ForceEnum.No, // forceWrites
                offsetBits,//
//                0, // readCacheCapacity
//                0, // readCacheMaxRecordSize
                true, // writeCacheEnabled
                false, // validateChecksum (desperation option for restart).
                createTime,//
                Quorum.NO_QUORUM,// Temporary stores are not HA.
                new ChecksumUtility(), // checker (root blocks generated but not saved).
                false // alternateRootBlock
        );
        
        buf = new DiskOnlyStrategy(maximumExtent, //
//                Long.valueOf(Options.DEFAULT_MINIMUM_EXTENSION), 
                md);

    }

    /**
     * Closes the store if it gets GCd.
     */
    protected void finalize() throws Throwable {
        
        try {
            
            synchronized (buf) {
            
                if (buf.isOpen()) {

                    close();

                    if (log.isInfoEnabled())
                        log.info("Finalized temp store");

                }

            }
            
        } catch (Throwable t) {
            
            t.printStackTrace(System.err);
            
        }
        
        super.finalize();
        
    }
    
    public String toString() {
        
        return getClass().getName() + "{file=" + getFile() + "}";
        
    }
    
    final public File getFile() {
        
        return buf.getFile();
        
    }

    /**
     * Close the store and delete the associated file, if any.
     */
    public void close() {

        synchronized (buf) {

            if (log.isInfoEnabled())
                log.info("Closing temp store");
            
            try {
            
                if (!buf.isOpen())
                    throw new IllegalStateException();

                buf.destroy();

            } finally {

//                if (writeCache != null) {
//
//                    try {
//
//                        DirectBufferPool.INSTANCE.release(writeCache);
//
//                        writeCache = null;
//
//                    } catch (Throwable t) {
//
//                        log.error(t, t);
//
//                    }
//
//                }

                if (LRUNexus.INSTANCE != null) {

                    try {

                        LRUNexus.INSTANCE.deleteCache(getUUID());

                    } catch (Throwable t) {

                        log.error(t, t);

                    }

                }

            }

        }

    }

    /**
     * Note: This operation is a NOP since {@link #close()} always deletes the
     * backing file and {@link #deleteResources()} requires that the store is
     * closed as a pre-condition.
     */
    public void deleteResources() {

        synchronized (buf) {

            if (buf.isOpen()) {

                throw new IllegalStateException();

            }

            /*
             * NOP
             */

        }
        
    }

    /**
     * Note: Temporary stores do not have persistent resource descriptions.
     */
    final public IResourceMetadata getResourceMetadata() {
        
        final File file = buf.getFile();
        
        final String fileStr = file == null ? "" : file.toString();
        
        return new ResourceMetadata(this, fileStr);
        
    }

    /**
     * Static class since must be {@link Serializable}.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    static final class ResourceMetadata extends AbstractResourceMetadata {

        /**
         * De-serializator ctor.
         */
        public ResourceMetadata() {
            
        }

        public ResourceMetadata(final TemporaryRawStore store,
                final String fileStr) {

            super(fileStr, // store.buf.getExtent()
                    store.uuid,//
                    store.createTime, //
                    0L// commitTime
            );

        }

        private static final long serialVersionUID = 1L;

        public boolean isJournal() {
            
            return false;
            
        }

        public boolean isIndexSegment() {

            return false;
            
        }

    }

    final public DiskOnlyStrategy getBufferStrategy() {

        return buf;

    }

    /**
     * Simply delegates to {@link #close()} since {@link #close()} always
     * deletes the backing file for a temporary store.
     */
    final public void destroy() {

        if(isOpen())
            close();

    }

    final public void force(boolean metadata) {
        
        buf.force(metadata);
        
    }

    final public long size() {
        
        return buf.size();
        
    }
    
    final protected void assertOpen() {
        
        if (!isOpen())
            throw new IllegalStateException();
        
    }
    
    final public boolean isOpen() {
        
        return buf.isOpen();
        
    }

    final public boolean isReadOnly() {
        
        return buf.isReadOnly();
        
    }
    
    /**
     * Always returns <code>false</code> since the store will be deleted as soon
     * as it is closed.
     */
    final public boolean isStable() {
    
        if (!isOpen())
            throw new IllegalStateException();

        return false;
        
    }

    /**
     * Return <code>false</code> since the temporary store is (at least in
     * principle) backed by disk.
     */
    final public boolean isFullyBuffered() {
        
        if (!isOpen())
            throw new IllegalStateException();
        
        return false;
        
    }

    final public ByteBuffer read(long addr) {

        return buf.read(addr);
        
    }

    final public long write(ByteBuffer data) {
    
        return buf.write(data);
                
    }

//    final public long allocate(int nbytes) {
//
//        return buf.allocate(nbytes);
//        
//    }
//
//    final public void update(long addr, int off, ByteBuffer data) {
//
//        buf.update(addr, off, data);
//        
//    }

    /**
     * The maximum length of a record that may be written on the store.
     */
    final public int getMaxRecordSize() {

        return ((AbstractRawWormStore) buf).getAddressManager()
                .getMaxByteCount();

    }

    public CounterSet getCounters() {

        return buf.getCounters();
        
    }

	@Override
	public void delete(long addr) {
		// void
		
	}

}
