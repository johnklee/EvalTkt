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
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import utils.FUM;


public class Eval {
	public static Logger logger = Logger.getLogger(Eval.class);
	
	// Output Identification Level Detail Log
	public static String OutputIL(JList<String> list, HashMap<String,List<String>> rtMap) {
		StringBuffer outBuf = new StringBuffer("");
		
		if(list.size()>0)
		{
			String id = list.get(0);
			outBuf.append(String.format("(%s, %s) ", id, rtMap.get(id).get(1)));
			for(int i=1; i<list.size(); i++) {
				id = list.get(i);
				outBuf.append(String.format("| (%s, %s) ", id, rtMap.get(id).get(1)));
			}
		}	
		
		return outBuf.toString();
	}
	
	// Output Position Level Detail Log
	public static String OutputPL(JList<String> list, HashMap<String,List<String>> rtMap) {
		StringBuffer outBuf = new StringBuffer("");
		
		if(list.size()>0)
		{
			String id = list.get(0);
			String result = "";
			if(!rtMap.get(id).get(0).isEmpty())
			{
				result = rtMap.get(id).toString().replaceAll("[\\[\\]]", "");
			}
			else
			{
				result = rtMap.get(id).get(1);
			}
			outBuf.append(String.format("(%s, %s) ", id, result));
			for(int i=1; i<list.size(); i++) {
				id = list.get(i);
				if(!rtMap.get(id).get(0).isEmpty())
				{
					result = rtMap.get(id).toString().replaceAll("[\\[\\]]", "");
				}
				else
				{
					result = rtMap.get(id).get(1);
				}
				outBuf.append(String.format("| (%s, %s) ", id, result));
			}
		}	
		
		return outBuf.toString();
	}
	
	/**
	 * REF:
	 * 	- Macro/Micro averaging: 
	 *    http://datamin.ubbcluj.ro/wiki/index.php/Evaluation_methods_in_text_categorization
	 *    http://rushdishams.blogspot.tw/2011/08/micro-and-macro-average-of-precision.html
	 *  - 2016 Shared Task for Chinese Grammatical Error Diagnosis (CGED)
	 *    http://nlptea2016.weebly.com/shared-task.html
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
//		HashMap<String,String> gtMap = new HashMap<String,String>();		
//		HashMap<String,String> rtMap = new HashMap<String,String>();
		// NLPTEA15, additional position level
		HashMap<String,List<String>> gtMap = new HashMap<String,List<String>>();		
		HashMap<String,List<String>> rtMap = new HashMap<String,List<String>>();
		QSReader qsr = new QSReader(truth);
		qsr.skipCommentLine = qsr.skipEmptyLine = true;
		qsr.open();
		for(String line:qsr)
		{
			String items[] = line.split(",");
			String key = items[0].trim();
//			if(items.length==2)
//			{
//				gtMap.put(key, items[1].trim());
//			}
			// NLPTEA15, additional position level
			if(items.length==4)
			{
				String pos = items[1].trim()+", "+items[2].trim();
				String err = items[3].trim();
				List<String> val = new ArrayList<String>();
				//eq test
//				String[] s1 = {"1", "1"};
//				String[] s2 = {"1", "1"};
//				logger.debug(String.format("\t[Debug] s1 same to s2? %s!\n", s1.equals(s2)));
//				List<String> a1 = new ArrayList<String>();
//				List<String> a2 = new ArrayList<String>();
//				a1.add("1"); a1.add("1");
//				a2.add("1"); a2.add("1");
//				logger.debug(String.format("\t[Debug] a1 same to a2? %s!\n", a1.equals(a2)));
//				return;
				val.add(pos);
				val.add(err);
				gtMap.put(key, val);
			}
			else if(items.length==2)
			{
				String pos = "";
				String err = items[1].trim();
				List<String> val = new ArrayList<String>();
				val.add(pos);
				val.add(err);
				gtMap.put(key, val);
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
			String key = items[0].trim();
//			if(items.length==2)
//			{
//				rtList.add(key);
//				if(rtMap.put(key, items[1].trim())!=null) logger.warn(String.format("\t[Warn] Duplicate item=%s!", key));
//			}
			// NLPTEA15, additional position level
			if(items.length==4)
			{
				String pos = items[1].trim()+", "+items[2].trim();
				String err = items[3].trim();
				List<String> val = new ArrayList<String>();
				val.add(pos);
				val.add(err);
				rtList.add(key);
				if(rtMap.put(key, val)!=null) logger.warn(String.format("\t[Warn] Duplicate item=%s!", key));
			}
			else if(items.length==2)
			{
				String pos = "";
				String err = items[1].trim();
				List<String> val = new ArrayList<String>();
				val.add(pos);
				val.add(err);
				rtList.add(key);
				if(rtMap.put(key, val)!=null) logger.warn(String.format("\t[Warn] Duplicate item=%s!", key));
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
		
		JList<String> pfpSet = new JList<String>();
		JList<String> pfnSet = new JList<String>();
		JList<String> ptpSet = new JList<String>();
		JList<String> ptnSet = new JList<String>();
		
		for(String id:rtList)
		{
			String gt = gtMap.get(id).get(1);
			String rt = rtMap.get(id).get(1);
			// NLPTEA15, additional position level
			String gt_pos = gtMap.get(id).get(0);
			String rt_pos = rtMap.get(id).get(0);
			if(gt.equalsIgnoreCase("correct"))
			{
				if(gt.equalsIgnoreCase(rt))
				{
					dtnSet.add(id);
					itnSet.add(id);
					if(gt_pos.equals(rt_pos))
					{
						ptnSet.add(id);
					}
					else
					{
						pfpSet.add(id);
					}
				}
				else
				{
					dfpSet.add(id);
					ifpSet.add(id);
					pfpSet.add(id);
				}
			}
			else // gt is positive
			{
				if(rt.equalsIgnoreCase("correct"))
				{
					dfnSet.add(id);
					ifnSet.add(id);
					pfnSet.add(id);
				}
				else if(rt.equalsIgnoreCase(gt))
				{
					dtpSet.add(id);
					itpSet.add(id);
					if(gt_pos.equals(rt_pos))
					{
						ptpSet.add(id);
					}
					else
					{
						pfnSet.add(id);
					}
				}
				else
				{
					// Both result/ground truth say not correct but not the same
					dtpSet.add(id);
					ifnSet.add(id);
					pfnSet.add(id);
				}
			}
		}
		qsr.close();
		
		// 3) Calculate Statistic Information
		/*QSWriter qsw = new QSWriter(output);
		if(output!=null) qsw = new QSWriter(output);
		else qsw = new QSWriter();*/
		StringBuffer outputBuf = new StringBuffer("NLPTEA 2015 Shared Task"+Envset.BreakLine);
		outputBuf.append("Chinese Grammatical Error Diagnosis"+Envset.BreakLine+Envset.BreakLine);
		
		double fp = ((double)dfpSet.size())/(dfpSet.size()+dtnSet.size());
		double daccuracy = ((double)dtpSet.size()+dtnSet.size())/rtList.size();
		double dprecision = ((double)dtpSet.size())/(dtpSet.size()+dfpSet.size());
		double drecall = ((double)dtpSet.size())/(dtpSet.size()+dfnSet.size());
		double df1Score = 2*(dprecision*drecall)/(dprecision+drecall);
		double iaccuracy = ((double)itpSet.size()+itnSet.size())/rtList.size();
		double iprecision = ((double)itpSet.size())/(itpSet.size()+ifpSet.size());
		double irecall = ((double)itpSet.size())/(itpSet.size()+ifnSet.size());
		double if1Score = 2*(iprecision*irecall)/(iprecision+irecall);
		// NLPTEA15, additional position level
		double paccuracy = ((double)ptpSet.size()+ptnSet.size())/rtList.size();
		double pprecision = ((double)ptpSet.size())/(ptpSet.size()+pfpSet.size());
		double precall = ((double)ptpSet.size())/(ptpSet.size()+pfnSet.size());
		double pf1Score = 2*(pprecision*precall)/(pprecision+precall);
		
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
		logger.debug("\t[Info] Identification Level:");
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
		
		// NLPTEA15, additional position level
		outputBuf.append("Position Level"+Envset.BreakLine);
		logger.debug("\t[Info] Position Level:");
		outputBuf.append(String.format("\tAccuracy = %s (%d/%d)%s", FUM.DoubleToStr(paccuracy), 
                                                                    ptpSet.size()+ptnSet.size(),
                                                                    rtList.size(),
                                                                    Envset.BreakLine));
		logger.debug(String.format("\t\tAccuracy=%s", FUM.DoubleToStr(paccuracy)));
		outputBuf.append(String.format("\tPrecision = %s (%d/%d)%s", FUM.DoubleToStr(pprecision), 
                                                                     ptpSet.size(),
                                                                     ptpSet.size()+pfpSet.size(),
                                                                     Envset.BreakLine));
		logger.debug(String.format("\t\tPrecision=%s", FUM.DoubleToStr(pprecision)));
		outputBuf.append(String.format("\tRecall = %s (%d/%d)%s", FUM.DoubleToStr(precall), 
                                                                  ptpSet.size(),
                                                                  ptpSet.size()+pfnSet.size(),
                                                                  Envset.BreakLine));
		logger.debug(String.format("\t\tRecall=%s", FUM.DoubleToStr(precall)));
		outputBuf.append(String.format("\tF1-Score = %s ((2*%s*%s)/(%s+%s))%s", 
				FUM.DoubleToStr(pf1Score), 
				FUM.DoubleToStr(pprecision),
				FUM.DoubleToStr(precall),
				FUM.DoubleToStr(pprecision),
				FUM.DoubleToStr(precall),
                Envset.BreakLine));
		logger.debug(String.format("\t\tF1-Score=%s", FUM.DoubleToStr(pf1Score)));
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
			qsw.line("Identification Level");
			qsw.line("");
			qsw.line("\t#TP:");
			qsw.line(String.format("\t%s", OutputIL(itpSet,rtMap)));
			qsw.line("");
			qsw.line("\t#FP:");
			qsw.line(String.format("\t%s", OutputIL(ifpSet,rtMap)));
			qsw.line("");
			qsw.line("\t#TN:");
			qsw.line(String.format("\t%s", OutputIL(itnSet,rtMap)));
			qsw.line("");
			qsw.line("\t#FN:");
			qsw.line(String.format("\t%s", OutputIL(ifnSet,rtMap)));
			qsw.line("");
			// NLPTEA15, additional position level
			qsw.line("Position Level");
			qsw.line("");
			qsw.line("\t#TP:");
			qsw.line(String.format("\t%s", OutputPL(ptpSet,rtMap)));
			qsw.line("");
			qsw.line("\t#FP:");
			qsw.line(String.format("\t%s", OutputPL(pfpSet,rtMap)));
			qsw.line("");
			qsw.line("\t#TN:");
			qsw.line(String.format("\t%s", OutputPL(ptnSet,rtMap)));
			qsw.line("");
			qsw.line("\t#FN:");
			qsw.line(String.format("\t%s", OutputPL(pfnSet,rtMap)));
			qsw.line("");
			qsw.close();
		}
	}
}
