package nlptea;

import flib.env.Envset;
import flib.util.JList;
import flib.util.io.QSReader;
import flib.util.io.QSWriter;
import gays.tools.ArguConfig;
import gays.tools.ArguParser;
import gays.tools.enums.EArguQuantity;
import gays.tools.enums.EArguRestrict;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import utils.FUM;


public class Eval {
	public static Logger logger = Logger.getLogger(Eval.class);
	
	/**
	 * REF:
	 * 	- Macro/Micro averaging: 
	 *    http://datamin.ubbcluj.ro/wiki/index.php/Evaluation_methods_in_text_categorization
	 *    http://rushdishams.blogspot.tw/2011/08/micro-and-macro-average-of-precision.html
	 * @param args
	 */
	public static void main(String args[]) throws Exception
	{
		File log4jCfg = new File("log4j.properties");
		if(log4jCfg.exists())
		{
			System.out.printf("\t[Info] Loading %s...\n", log4jCfg.getAbsolutePath());
			PropertyConfigurator.configure(log4jCfg.getAbsolutePath());  
		}
		else
		{
			Properties props = new Properties();
			props.setProperty("log4j.rootLogger", "INFO, A1");
			props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
			props.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
			props.setProperty("log4j.appender.A1.layout.ConversionPattern", "%m%n");			
			PropertyConfigurator.configure(props);
		}
		
		HashMap<String, Object> defineOfArgu = new HashMap<String, Object>();						
		defineOfArgu.put("-i,--Input", new ArguConfig("file path of the system results (required)", EArguQuantity.SINGLE, EArguRestrict.Required));
		defineOfArgu.put("-t,--Truth", new ArguConfig("file path of the ground truth (required)", EArguQuantity.SINGLE, EArguRestrict.Required));		
		defineOfArgu.put("-o,--Output", new ArguConfig("file path of the detailed evaluation report (optional)", EArguQuantity.SINGLE));
		
		ArguParser parser = new ArguParser(defineOfArgu, args);		
		/*parser.arguRegList.add("-i");
		parser.arguRegList.add("-t");
		parser.arguRegList.add("-o");*/
		parser.showAA = false;
		
		if(args.length==0)
		{
			parser.showArgus();
			System.out.println();
			return;
		}
		
		// 0) Reading setting
		File input = null, truth = null, output = null;

		if (parser.isSet("-i")) {
			input = new File(parser.getArguValue("-i"));
			if (!input.exists()) {
				logger.error(String.format("\t[Error] Input File='%s' doesn't exist!\n", input.getAbsolutePath()));				
				return;
			}
		} else {
			logger.error(String.format("\t[Error] Argument '-i' is required!\n"));
			parser.showArgus();
			System.out.println();
			return;
		}

		if (parser.isSet("-t")) {
			truth = new File(parser.getArguValue("-t"));
			if (!truth.exists()) {
				logger.error(String.format("\t[Error] Truth File='%s' doesn't exist!\n",truth.getAbsolutePath()));				
				return;
			}
		} else {
			logger.error(String.format("\t[Error] Argument '-t' is required!\n"));			
			parser.showArgus();
			System.out.println();
			return;
		}
		
		if (parser.isSet("-o")) {
			output = new File(parser.getArguValue("-o"));
			output.createNewFile();
		}
		
		// 1) Start reading result/truth	
		List<String> rtList = new ArrayList<String>();
		HashMap<String,String> gtMap = new HashMap<String,String>();		
		HashMap<String,String> rtMap = new HashMap<String,String>();
		QSReader qsr = new QSReader(truth);
		qsr.skipCommentLine = qsr.skipEmptyLine = true;
		qsr.open();
		for(String line:qsr)
		{
			String items[] = line.split(",");
			String key = items[0].trim().toLowerCase();
			if(items.length==2)
			{
				gtMap.put(key, items[1].trim());
			}
			else
			{
				logger.error(String.format("\t[Error] Illegal Ground Truth Input='%s'!\n", line));
				return;
			}
		}
		logger.debug(String.format("\t[Info] Analyze Ground Truth...(%d)", gtMap.size()));
		qsr.reopen(input);
		for(String line:qsr)
		{
			String items[] = line.split(",");
			if(items.length==2)
			{
				String key = items[0].trim().toLowerCase();
				rtList.add(key);
				if(rtMap.put(key, items[1].trim())!=null) logger.warn(String.format("\t[Warn] Duplicate item=%s!", key));
			}
			else
			{
				logger.error(String.format("\t[Error] Illegal System Result Input='%s'!\n", line));
				return;
			}
		}
		logger.debug(String.format("\t[Info] Analyze System Result...(%d)", rtMap.size()));
		if(rtMap.size()!=gtMap.size())
		{
			logger.error(String.format("\t[Error] Unbalanced!"));
			if(rtMap.size()>gtMap.size())
			{
				for(String key:rtMap.keySet())
				{
					if(!gtMap.containsKey(key)) logger.error(String.format("\t\tMissing %s in ground truth...", key));
				}
			}
			else
			{
				for(String key:gtMap.keySet())
				{
					if(!rtMap.containsKey(key)) logger.error(String.format("\t\tMissing %s in answer", key));
				}
			}
			logger.error("");
			return;
		}
		
		// 2) Start evaluation
		JList<String> dfpSet = new JList<String>();
		JList<String> dfnSet = new JList<String>();
		JList<String> dtpSet = new JList<String>();
		JList<String> dtnSet = new JList<String>();
		JList<String> ifpSet = new JList<String>();
		JList<String> ifnSet = new JList<String>();
		JList<String> itpSet = new JList<String>();
		JList<String> itnSet = new JList<String>();
		
		for(String id:rtList)
		{
			String gt = gtMap.get(id);
			String rt = rtMap.get(id);
			if(gt.equals("correct"))
			{
				if(gt.equals(rt))
				{
					dtnSet.add(id);
					itnSet.add(id);
				}
				else
				{
					dfpSet.add(id);
					ifpSet.add(id);
				}
			}
			else // gt is positive
			{
				if(rt.equals("correct"))
				{
					dfnSet.add(id);
					ifnSet.add(id);
				}
				else if(rt.equals(gt))
				{
					dtpSet.add(id);
					itpSet.add(id);
				}
				else
				{
					// Both result/ground truth say not correct but not the same
					dtpSet.add(id);
					ifnSet.add(id);
				}
			}
		}
		qsr.close();
		
		// 3) Calculate Statistic Information
		/*QSWriter qsw = new QSWriter(output);
		if(output!=null) qsw = new QSWriter(output);
		else qsw = new QSWriter();*/
		StringBuffer outputBuf = new StringBuffer("NLPTEA 2014 Shared Task"+Envset.BreakLine);
		outputBuf.append("Grammatical Error Diagnosis for Learning Chinese as a Foreign Language"+Envset.BreakLine+Envset.BreakLine);
		
		double fp = ((double)dfpSet.size())/(dfpSet.size()+dtnSet.size());
		double daccuracy = ((double)dtpSet.size()+dtnSet.size())/rtList.size();
		double dprecision = ((double)dtpSet.size())/(dtpSet.size()+dfpSet.size());
		double drecall = ((double)dtpSet.size())/(dtpSet.size()+dfnSet.size());
		double df1Score = 2*(dprecision*drecall)/(dprecision+drecall);
		double iaccuracy = ((double)itpSet.size()+itnSet.size())/rtList.size();
		double iprecision = ((double)itpSet.size())/(itpSet.size()+ifpSet.size());
		double irecall = ((double)itpSet.size())/(itpSet.size()+ifnSet.size());
		double if1Score = 2*(iprecision*irecall)/(iprecision+irecall);
		
		outputBuf.append("=========================================================="+Envset.BreakLine);
		outputBuf.append("Part 1: Overall Performance"+Envset.BreakLine);
		outputBuf.append("=========================================================="+Envset.BreakLine+Envset.BreakLine);
		
		//False Positive Rate = 0.5 (2/4)
		outputBuf.append(String.format("False Positive Rate = %s (%d/%d)%s%s", FUM.DoubleToStr(fp), 
				                                                             dfpSet.size(),
				                                                             dfpSet.size()+dtnSet.size(),
				                                                             Envset.BreakLine,
				                                                             Envset.BreakLine));		
		logger.debug(String.format("\t[Info] FP=%s\n", FUM.DoubleToStr(fp)));
		outputBuf.append("Detection Level"+Envset.BreakLine);
		logger.debug("\t[Info] Detection Level:");
		// Accuracy = 0.7 (7/10)
		outputBuf.append(String.format("\tAccuracy = %s (%d/%d)%s", FUM.DoubleToStr(daccuracy), 
				                                                    dtpSet.size()+dtnSet.size(),
				                                                    rtList.size(),
				                                                    Envset.BreakLine));
		logger.debug(String.format("\t\tAccuracy=%s", FUM.DoubleToStr(daccuracy)));
		// 	Precision = 0.7143 (5/7)
		outputBuf.append(String.format("\tPrecision = %s (%d/%d)%s", FUM.DoubleToStr(dprecision), 
				                                                     dtpSet.size(),
                                                                     dtpSet.size()+dfpSet.size(),
                                                                     Envset.BreakLine));
		logger.debug(String.format("\t\tPrecision=%s", FUM.DoubleToStr(dprecision)));
		// Recall = 0.8333 (5/6)
		outputBuf.append(String.format("\tRecall = %s (%d/%d)%s", FUM.DoubleToStr(drecall), 
				                                                  dtpSet.size(),
				                                                  dtpSet.size()+dfnSet.size(),
                                                                  Envset.BreakLine));
		logger.debug(String.format("\t\tRecall=%s", FUM.DoubleToStr(drecall)));
		// 	F1-Score = 0.7692 ((2*0.7143*0.8333)/(0.7143+0.8333))
		outputBuf.append(String.format("\tF1-Score = %s ((2*%s*%s)/(%s+%s))%s", 
				FUM.DoubleToStr(df1Score), 
				FUM.DoubleToStr(dprecision),
				FUM.DoubleToStr(drecall),
				FUM.DoubleToStr(dprecision),
				FUM.DoubleToStr(drecall),
                Envset.BreakLine));
		logger.debug(String.format("\t\tF1-Score=%s\n", FUM.DoubleToStr(df1Score)));
		outputBuf.append(Envset.BreakLine);
		
		outputBuf.append("Identification Level"+Envset.BreakLine);
		logger.debug("\t[nfo] Identification Level:");
		outputBuf.append(String.format("\tAccuracy = %s (%d/%d)%s", FUM.DoubleToStr(iaccuracy), 
                                                                    itpSet.size()+itnSet.size(),
                                                                    rtList.size(),
                                                                    Envset.BreakLine));
		logger.debug(String.format("\t\tAccuracy=%s", FUM.DoubleToStr(iaccuracy)));
		outputBuf.append(String.format("\tPrecision = %s (%d/%d)%s", FUM.DoubleToStr(iprecision), 
                                                                     itpSet.size(),
                                                                     itpSet.size()+ifpSet.size(),
                                                                     Envset.BreakLine));
		logger.debug(String.format("\t\tPrecision=%s", FUM.DoubleToStr(iprecision)));
		outputBuf.append(String.format("\tRecall = %s (%d/%d)%s", FUM.DoubleToStr(irecall), 
                                                                  itpSet.size(),
                                                                  itpSet.size()+ifnSet.size(),
                                                                  Envset.BreakLine));
		logger.debug(String.format("\t\tRecall=%s", FUM.DoubleToStr(irecall)));
		outputBuf.append(String.format("\tF1-Score = %s ((2*%s*%s)/(%s+%s))%s", 
				FUM.DoubleToStr(if1Score), 
				FUM.DoubleToStr(iprecision),
				FUM.DoubleToStr(irecall),
				FUM.DoubleToStr(iprecision),
				FUM.DoubleToStr(irecall),
                Envset.BreakLine));
		logger.debug(String.format("\t\tF1-Score=%s", FUM.DoubleToStr(if1Score)));
		outputBuf.append(Envset.BreakLine);		
		
		if(output==null)
		{
			System.out.printf("%s\n", outputBuf.toString());
		}
		else
		{
			QSWriter qsw = new QSWriter(output);
			qsw.line(outputBuf.toString());
			qsw.line("");
			qsw.line("==========================================================");
			qsw.line("Part 2: Details");
			qsw.line("==========================================================");
			qsw.line("");
			qsw.line("Detection Level");
			qsw.line("");
			qsw.line("\t#TP:");
			qsw.line(String.format("\t%s", dtpSet.join("|", " ")));
			qsw.line("");
			qsw.line("\t#FP:");
			qsw.line(String.format("\t%s", dfpSet.join("|", " ")));
			qsw.line("");
			qsw.line("\t#TN:");
			qsw.line(String.format("\t%s", dtnSet.join("|", " ")));
			qsw.line("");
			qsw.line("\t#FN:");
			qsw.line(String.format("\t%s", dfnSet.join("|", " ")));
			qsw.line("");
			qsw.line("");
			qsw.line("Identification Level");
			qsw.line("");
			qsw.line("\t#TP:");
			qsw.write("\t");
			if(itpSet.size()>0)
			{
				String id = itpSet.get(0);
				qsw.write(String.format("(%s, %s) ", id, rtMap.get(id)));
				for(int i=1; i<itpSet.size(); i++) {
					id = itpSet.get(i);
					qsw.write(String.format("| (%s, %s) ", id, rtMap.get(id)));
				}
				qsw.line("");
			}									
			qsw.line("");
			qsw.line("\t#FP:");
			qsw.write("\t");
			if(ifpSet.size()>0)
			{
				String id = ifpSet.get(0);
				qsw.write(String.format("(%s, %s) ", id, rtMap.get(id)));
				for(int i=1; i<ifpSet.size(); i++) {
					id = ifpSet.get(i);
					qsw.write(String.format("| (%s, %s) ", id, rtMap.get(id)));
				}
				qsw.line("");
			}
			qsw.line("");
			qsw.line("\t#TN:");
			qsw.write("\t");
			if(itnSet.size()>0)
			{
				String id = itnSet.get(0);
				qsw.write(String.format("(%s, %s) ", id, rtMap.get(id)));
				for(int i=1; i<itnSet.size(); i++) {
					id = itnSet.get(i);
					qsw.write(String.format("| (%s, %s) ", id, rtMap.get(id)));
				}
				qsw.line("");
			}
			qsw.line("");
			qsw.line("\t#FN:");
			qsw.write("\t");
			if(ifnSet.size()>0)
			{
				String id = ifnSet.get(0);
				qsw.write(String.format("(%s, %s) ", id, rtMap.get(id)));
				for(int i=1; i<ifnSet.size(); i++) {
					id = ifnSet.get(i);
					qsw.write(String.format("| (%s, %s) ", id, rtMap.get(id)));
				}
				qsw.line("");
			}
			qsw.line("");
			qsw.close();
		}
	}
}
