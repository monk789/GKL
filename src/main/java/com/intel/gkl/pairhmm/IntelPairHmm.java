package com.intel.gkl.pairhmm;

import com.intel.gkl.compression.IntelInflater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.intel.gkl.IntelGKLUtils;
import com.intel.gkl.NativeLibraryLoader;
import org.broadinstitute.gatk.nativebindings.pairhmm.HaplotypeDataHolder;
import org.broadinstitute.gatk.nativebindings.pairhmm.PairHMMNativeArguments;
import org.broadinstitute.gatk.nativebindings.pairhmm.PairHMMNativeBinding;
import org.broadinstitute.gatk.nativebindings.pairhmm.ReadDataHolder;

import java.io.File;

/**
 * Provides a native PairHMM implementation accelerated for the Intel Architecture.
 */
public class IntelPairHmm implements PairHMMNativeBinding {
    private final static Logger logger = LogManager.getLogger(IntelPairHmm.class);
    private static final String NATIVE_LIBRARY_NAME = "gkl_pairhmm";
    private String nativeLibraryName = "gkl_pairhmm";
    boolean useFpga = false;

    void setNativeLibraryName(String nativeLibraryName) {
        this.nativeLibraryName = nativeLibraryName;
    }

    public IntelPairHmm() {
        setNativeLibraryName(NATIVE_LIBRARY_NAME);
    }

    /**
     * Loads the native library, if it is supported on this platform. <p>
     * Returns false if AVX is not supported. <br>
     * Returns false if the native library cannot be loaded for any reason. <br>
     *
     * @param tempDir  directory where the native library is extracted or null to use the system temp directory
     * @return  true if the native library is supported and loaded, false otherwise
     */
    @Override
    public synchronized boolean load(File tempDir) {

        IntelGKLUtils utils = new IntelGKLUtils();

        boolean isLoaded = utils.load(null);

        if(!isLoaded)
        {
            logger.warn("Intel GKL Utils not loaded");
            return false;
        }

        if (!utils.isAvxSupported()) {
            return false;
        }
        return NativeLibraryLoader.load(tempDir, nativeLibraryName);
    }

    /**
     * Initialize native PairHMM with the supplied args.
     *
     * @param args the args used to configure native PairHMM
     */
    public void initialize(PairHMMNativeArguments args) {
        if (args == null) {
            args = new PairHMMNativeArguments();
            args.useDoublePrecision = false;
            args.maxNumberOfThreads = 1;
        }
        if (args.useDoublePrecision && useFpga) {
            logger.warn("FPGA PairHMM does not support double precision floating-point. Using AVX PairHMM");
        }
        initNative(ReadDataHolder.class, HaplotypeDataHolder.class, args.useDoublePrecision, args.maxNumberOfThreads, useFpga);
    }

    /**
     *
     *
     * @param readDataArray array of read data
     * @param haplotypeDataArray array of haplotype data
     * @param likelihoodArray array of double results
     */

    @Override
    public void computeLikelihoods(ReadDataHolder[] readDataArray,
                                   HaplotypeDataHolder[] haplotypeDataArray,
                                   double[] likelihoodArray)
    {
        computeLikelihoodsNative(readDataArray, haplotypeDataArray, likelihoodArray);
    }

    /**
     *
     */
    @Override
    public void done() {
        doneNative();
    }

    private native static void initNative(Class<?> readDataHolderClass,
                                          Class<?> haplotypeDataHolderClass,
                                          boolean doublePrecision,
                                          int maxThreads,
                                          boolean useFpga);

    private native void computeLikelihoodsNative(Object[] readDataArray,
                                                 Object[] haplotypeDataArray,
                                                 double[] likelihoodArray);

    private native void doneNative();
}
