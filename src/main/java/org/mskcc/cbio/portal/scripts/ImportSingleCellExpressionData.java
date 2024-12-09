package org.mskcc.cbio.portal.scripts;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.math.NumberUtils;
import org.mskcc.cbio.portal.model.Sample;
import org.mskcc.cbio.portal.dao.DaoCancerStudy;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoGeneOptimized;
import org.mskcc.cbio.portal.dao.DaoGeneticProfile;
import org.mskcc.cbio.portal.dao.DaoSample;
import org.mskcc.cbio.portal.dao.DaoSingleCellExpression;
import org.mskcc.cbio.portal.dao.JdbcUtil;
import org.mskcc.cbio.portal.model.CancerStudy;
import org.mskcc.cbio.portal.model.CanonicalGene;
import org.mskcc.cbio.portal.model.GeneticProfile;
import org.mskcc.cbio.portal.model.SingleCellExpression;
import org.mskcc.cbio.portal.util.ConsoleUtil;
import org.mskcc.cbio.portal.util.FileUtil;
import org.mskcc.cbio.portal.util.ProgressMonitor;
import org.mskcc.cbio.portal.util.StableIdUtil;
import org.mskcc.cbio.portal.util.TsvUtil;

public class ImportSingleCellExpressionData {

    private File dataFile;
    private int numLines;

    private int geneticProfileId;
    private DaoGeneOptimized daoGene;

    /**
     * Constructor.
     *
     * @param dataFile         Generic Assay Patient Level data file
     * @param geneticProfileId GeneticProfile ID.
     */
    public ImportSingleCellExpressionData(
        File dataFile,
        int geneticProfileId,
        DaoGeneOptimized daoGene
    ) {
        this.dataFile = dataFile;
        this.geneticProfileId = geneticProfileId;
        this.daoGene = daoGene;
    }

    /**
     * Import the SingleCellExpression Data
     */
    public void importData() {
        JdbcUtil.getTransactionTemplate().execute(status -> {
            try {
                doImportData();
            } catch (Throwable e) {
                status.setRollbackOnly();
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    private int sampleIdIndex;
    private int hugoSymbolIndex;
    private int entrezGeneIdIndex;
    private int cellTypeIndex;
    private int tissueIndex;
    private int valueIndex;
    private GeneticProfile geneticProfile;

    private void doImportData() throws IOException, DaoException {
        try {
            this.numLines = FileUtil.getNumLines(dataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("In doImportData singleCellExpression");
        ProgressMonitor.setMaxValue(numLines);
        FileReader reader = new FileReader(dataFile);
        BufferedReader buf = new BufferedReader(reader);
        String headerLine = buf.readLine();
        String[] headerParts = TsvUtil.splitTsvLine(headerLine);

        processHeader(headerParts);
        System.out.println("Header processed");

        int numRecordsToAdd = 0;
        int entriesSkipped = 0;
        String line;
        geneticProfile = DaoGeneticProfile.getGeneticProfileById(geneticProfileId);
        while ((line = buf.readLine()) != null) {
            System.out.println("processing line: " + line);
            ProgressMonitor.incrementCurValue();
            ConsoleUtil.showProgress();

            if (TsvUtil.isDataLine(line)) {
                System.out.println("is data line");
                String[] parts = TsvUtil.splitTsvLine(line);
                if (saveLine(parts)) {
                    System.out.println("Saved line");
                    numRecordsToAdd++;
                } else {
                    System.out.println("skipped");
                    entriesSkipped++;
                }
            }

        }
        buf.close();

        if (entriesSkipped > 0) {
            ProgressMonitor.setCurrentMessage(" --> total number of data entries skipped (see table below):  " + entriesSkipped);
        }

        if (numRecordsToAdd <= 0) {
            throw new DaoException ("Something has gone wrong!  I did not save any records" +
                    " to the database!");
        }
    }

    private boolean saveLine(String[] line) throws DaoException {
        SingleCellExpression singleCellExpression;
        System.out.println("Saving line: " + line);
        try {
            singleCellExpression = parseSingleCellExpression(line);
        } catch (DaoException e) {
            return false;
        }
        System.out.println("SingleCellExpression made: " + singleCellExpression.toString());

        DaoSingleCellExpression.addSingleCellExpression(singleCellExpression);
        System.out.println("Added to db");
        return true;
    }


    private SingleCellExpression parseSingleCellExpression(String[] line) throws DaoException {
        String hugoSymbol = (hugoSymbolIndex < 0) ? "" : line[hugoSymbolIndex];
        String entrezGeneId = (entrezGeneIdIndex < 0) ? "" : line[entrezGeneIdIndex];
        CanonicalGene gene = parseGene(hugoSymbol, entrezGeneId);
        // skip the record if a gene was expected but not identified
        if (gene == null) {
            ProgressMonitor.logWarning("Gene not found. Skipping line.");
            throw new DaoException("Gene not found");
        }
        Sample sample = DaoSample.getSampleByCancerStudyAndSampleId(geneticProfile.getCancerStudyId(), line[sampleIdIndex]);
        if (sample == null) {
            ProgressMonitor.logWarning("Sample \'" + line[sampleIdIndex] + "\' not found in sample file. Skipping line.");
            throw new DaoException("Sample not found");
        }

        return new SingleCellExpression(
            sample.getInternalId(),
            geneticProfileId,
            gene,
            line[cellTypeIndex],
            line[tissueIndex],
            line[valueIndex]
        );
    }


    private CanonicalGene parseGene(String geneSymbol, String entrezId) {
        //  Assume we are dealing with Entrez Gene Ids (this is the best / most stable option)

        CanonicalGene gene = null;
        // try to parse entrez if it is not empty nor 0:
        if (!(entrezId.isEmpty() ||
                entrezId.equals("0"))) {
            Long entrezGeneId;
            try {
                entrezGeneId = Long.parseLong(entrezId);
            } catch (NumberFormatException e) {
                entrezGeneId = null;
            }
            //non numeric values or negative values should not be allowed:
            if (entrezGeneId == null || entrezGeneId < 0) {
                ProgressMonitor.logWarning(
                        "Ignoring line with invalid Entrez_Id " +
                        entrezId);
                return gene;
            } else {
                gene = daoGene.getGene(entrezGeneId);
                if (gene == null) {
                    //skip if not in DB:
                    ProgressMonitor.logWarning(
                            "Entrez gene ID " + entrezGeneId +
                            " not found. Record will be skipped.");
                    return gene;
                }
            }
        }

        // If Entrez Gene ID Fails, try Symbol.
        if (gene == null &&
                !(geneSymbol.equals("") ||
                    geneSymbol.equals("Unknown"))) {
            gene = daoGene.getNonAmbiguousGene(geneSymbol, true);
        }

        if (gene == null) {
            ProgressMonitor.logWarning(
                    "Ambiguous or missing gene: " + geneSymbol +
                    " ["+ entrezId +
                    "] or ambiguous alias. Ignoring it " +
                    "and all mutation data associated with it!");
        }
        return gene;
    }

    private void processHeader(String[] header) {
        String error = "Missing";
        boolean issueWithHeader = false;

        sampleIdIndex = getColIndexByName(header, "Sample_Id");
        if (sampleIdIndex < 0) {
            error += " \'Sample_Id\'";
            issueWithHeader = true;
        }
        hugoSymbolIndex = getColIndexByName(header, "Hugo_Symbol");
        entrezGeneIdIndex = getColIndexByName(header, "Entrez_Gene_Id");
        if (hugoSymbolIndex < 0 && entrezGeneIdIndex < 0) {
            if (issueWithHeader) {
                error += ",";
            }
            error += " \'Hugo_Symbol\' or \'Entrez_Gene_Id\'";
            issueWithHeader = true;
        }
        cellTypeIndex = getColIndexByName(header, "Cell_Type");
        if (cellTypeIndex < 0) {
            if (issueWithHeader) {
                error += ",";
            }
            error += " \'Cell_Type\'";
            issueWithHeader = true;
        }
        tissueIndex = getColIndexByName(header, "Tissue");
        if (tissueIndex < 0) {
            if (issueWithHeader) {
                error += ",";
            }
            error += " \'Tissue\'";
            issueWithHeader = true;
        }
        valueIndex = getColIndexByName(header, "Expression_Value");
        if (valueIndex < 0) {
            if (issueWithHeader) {
                error += ",";
            }
            error += " \'Expression_Value\'";
            issueWithHeader = true;
        }
        if (issueWithHeader) {
           throw new RuntimeException(error + "columns. Please fix your file.");
        }
    }

    // helper function for finding the index of a column by name
    private int getColIndexByName(String[] headers, String colName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase(colName)) {
                return i;
            }
        }
        return -1;
    }

}
