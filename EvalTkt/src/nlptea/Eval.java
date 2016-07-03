package nlptea;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import utils.FUM;
import flib.env.Envset;
import flib.util.JList;
import flib.util.io.QSReader;
import flib.util.io.QSWriter;
import gays.tools.ArguConfig;
import gays.tools.ArguParser;
import gays.tools.enums.EArguQuantity;
import gays.tools.enums.EArguRestrict;


public class Eval {
	public static Logger logger = Logger.getLogger(Eval.class);
	public static String CORRECT = "CORRECT";
	public static enum ET{TP, TN, FP, FN} 
	
	
	/**
	 * Join element of Set as String with separator ','
	 * 
	 * @param set
	 * @return
	 */
	public static String JST(Set<String> set, String sep)
	{
		StringBuffer strBuf = new StringBuffer("");
		if(set.size()>0)
		{
			Iterator<String> iter = set.iterator();
			strBuf.append(iter.next());
			while(iter.hasNext())
			{
				strBuf.append(String.format(" %s %s",  sep, iter.next()));
			}
		}
		return strBuf.toString();
	}
	public static String JST(Set<String> set) {return JST(set, ",");}
	
	// Output Identification Level Detail Log
	public static String OutputIL(JList<String> list, 
								  HashMap<String,Map<String,Set<String>>> rtMap,
								  HashMap<String,Map<String,Set<String>>> gtMap,
								  ET type) {
		StringBuffer outBuf = new StringBuffer("");
		Set<String> rSet = new TreeSet<String>();
		Set<String> gemSet = null, remSet = null;
		for(String id:list) {			
			switch(type)
			{
			case TP:
				gemSet = gtMap.get(id).keySet();
				for(String e:rtMap.get(id).keySet())
				{
					if(gemSet.contains(e)) rSet.add(String.format("%s, %s", id, e));
				}
				break;
			case TN:
				rSet.add(String.format("%s, %s", id, CORRECT));
				break;
			case FP:
				gemSet = gtMap.get(id).keySet();
				for(String e:rtMap.get(id).keySet())
				{
					if(!gemSet.contains(e)) rSet.add(String.format("%s, %s", id, e));
				}
				break;
			case FN:
				remSet = rtMap.get(id).keySet();
				if(remSet.size()==1 && remSet.contains(CORRECT)) rSet.add(String.format("%s, %s",  id, CORRECT));
				break;
			}
		}	
		
		return JST(rSet, "|");
	}
		
	public static boolean IsCorrect(Map<String,Set<String>> map)
	{
		if(map.size()==1 && map.keySet().contains(CORRECT)) return true;
		return false;
	}
	
	/**
	 * Identification level: this level could be considered as a multi-class categorization problem. 
	 * In addition to correct instances, all error types should be clearly identified.
	 * 
	 * @param gm
	 * @param rm
	 * @return
	 */
	public static boolean IDCheck(Map<String,Set<String>> gm, Map<String,Set<String>> rm)
	{
		if(gm.size()==rm.size() && gm.keySet().containsAll(rm.keySet())) return true;
		return false;
	}
	
	/**
	 * Position level: besides identifying the error types, this level also judges the positions of erroneous range. 
	 * That is, the system results should be perfectly identical with the quadruples of gold standard.
	 * 
	 * @param gm
	 * @param rm
	 * @return
	 */
	public static boolean POSCheck(Map<String,Set<String>> gm, Map<String,Set<String>> rm)
	{
		Iterator<Entry<String,Set<String>>> gmIter = gm.entrySet().iterator();
		while(gmIter.hasNext())
		{
			Entry<String,Set<String>> ety = gmIter.next();
			Set<String> gmPosSet = ety.getValue();
			Set<String> rmPosSet = rm.get(ety.getKey());
			if(gmPosSet.size()!=rmPosSet.size() || (!gmPosSet.containsAll(rmPosSet))) return false;
		}
		return true;
	}
	
	public static String ErrPos2Str(Map<String,Set<String>> epm)
	{		
		Set<String> es = new TreeSet<String>();
		if(epm.size()>0)
		{
			Iterator<Entry<String,Set<String>>> epmIter = epm.entrySet().iterator();
			while(epmIter.hasNext())
			{
				Entry<String,Set<String>> ety = epmIter.next();				
				if(ety.getValue().size()==0) es.add(String.format("%s", ety.getKey()));
				else es.add(String.format("%s(%s)", ety.getKey(), JST(ety.getValue(), "/")));
			}			
		}
		return JST(es);
	}
	
	// Output Position Level Detail Log
	public static String OutputPL(JList<String> list, 
								  HashMap<String,Map<String,Set<String>>> rtMap, 
								  HashMap<String,Map<String,Set<String>>> gtMap, 
								  ET type) {
		StringBuffer outBuf = new StringBuffer("");
		Set<String> rSet = new TreeSet<String>();
		if(list.size()>0)
		{			
			for(String id:list) {
				Map<String,Set<String>> rem = rtMap.get(id);
				Map<String,Set<String>> gem = gtMap.get(id);
				Iterator<Entry<String,Set<String>>> emIter = null, gemIter = null;
				switch(type)
				{
				case TP:
					emIter = rem.entrySet().iterator();
					while(emIter.hasNext())
					{
						Entry<String,Set<String>> e = emIter.next();
						Set<String> gpos = gem.get(e.getKey());
						for(String pos:e.getValue())
						{
							if(gpos!=null && gpos.contains(pos)) rSet.add(String.format("%s, %s, %s", id, pos, e.getKey()));
						}
					}
					break;
				case TN:
					rSet.add(String.format("%s, %s",  id, CORRECT));
					break;
				case FP:
					emIter = rem.entrySet().iterator();
					while(emIter.hasNext())
					{
						Entry<String,Set<String>> e = emIter.next();
						Set<String> gpos = gem.get(e.getKey());
						for(String pos:e.getValue())
						{
							if(gpos==null || !gpos.contains(pos)) rSet.add(String.format("%s, %s, %s", id, pos, e.getKey()));
						}
					}
					break;
				case FN:
					if(rem.size()==1 && rem.keySet().contains(CORRECT)) rSet.add(String.format("%s, %s",  id, CORRECT));
					break;
				}
			}
		}	
		
		return JST(rSet, "|");
	}
	
	// Translate Position Pair Into String
	public static String Pos2Str(String i, String j)
	{
		return String.format("%s, %s",  i, j);
	}
	
	/**
	 * Acquire Error-Position Map  
	 * 
	 * @param map: ID-Error Map with key as ID (e.g. B1-0201-1) and value as Map of Error Position Map
	 * @param key: ID
	 * @return
	 */
	public static Map<String,Set<String>> AcquireEM(HashMap<String,Map<String,Set<String>>> map, String key)
	{
		Map<String,Set<String>> vm = map.get(key);
		if(vm==null)
		{
			vm = new TreeMap<String,Set<String>>();
			map.put(key, vm);
		}
		return vm;
	}
	
	/**
	 * Acquire specific Error-Position Set.
	 * 
	 * @param em: Error-Position Map with key as "Error Type" (R,M,S,W) and value as position Set.
	 * @param key: Error type (R,M,S,W)
	 * @return
	 */
	public static Set<String> AcquireEPS(Map<String,Set<String>> em, String key)
	{
		Set<String> eps = em.get(key);
		if(eps==null)
		{
			eps = new TreeSet<String>();
			em.put(key, eps);
		}
		return eps;
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
		Set<String> idSet = new TreeSet<String>();

		// NLPTEA15, additional position level
		HashMap<String,Map<String,Set<String>>> gtMap = new HashMap<String,Map<String,Set<String>>>();		
		HashMap<String,Map<String,Set<String>>> rtMap = new HashMap<String,Map<String,Set<String>>>();
		QSReader qsr = new QSReader(truth);
		qsr.skipCommentLine = qsr.skipEmptyLine = true;
		qsr.open();
		for(String line:qsr)
		{
			String items[] = line.split(",");
			String key = items[0].trim();			

			// NLPTEA15, additional position level
			if(items.length==4)
			{
				// e.g. "B1-0201-1, 1, 1, M"
				Map<String,Set<String>> em = AcquireEM(gtMap, key); 
				String pos = Pos2Str(items[1].trim(), items[2].trim());
				String err = items[3].trim().toUpperCase();
				Set<String> eps = AcquireEPS(em, err);
				eps.add(pos);				
			}
			else if(items.length==2)
			{
				// e.g. "B2-1444-1, correct"
				Map<String,Set<String>> em = AcquireEM(gtMap, key); 
				//String pos = "";
				String err = items[1].trim().toUpperCase();
				Set<String> eps = AcquireEPS(em, err);
				//eps.add(pos);	
			}
			else
			{
				logger.error(String.format("\t[Error] Illegal Ground Truth Input='%s'!\n", line));
				return;
			}
		}
		logger.debug(String.format("\t[Info] Analyzed Ground Truth...(%d)", gtMap.size()));
		qsr.reopen(input);
		for(String line:qsr)
		{
			String items[] = line.split(",");
			String key = items[0].trim();
			idSet.add(key);
			// NLPTEA15, additional position level
			if(items.length==4)
			{
				Map<String,Set<String>> em = AcquireEM(rtMap, key); 
				String pos = Pos2Str(items[1].trim(), items[2].trim());
				String err = items[3].trim().toUpperCase();
				Set<String> eps = AcquireEPS(em, err);
				if(!eps.add(pos)) logger.warn(String.format("\t[Warn] Duplicate Error Item=(%s,%s=(%s))!", key, err, key));						
			}
			else if(items.length==2)
			{
				Map<String,Set<String>> em = AcquireEM(rtMap, key); 
				String pos = "";
				String err = items[1].trim().toUpperCase();
				Set<String> eps = AcquireEPS(em, err);
				//if(!eps.add(pos)) logger.warn(String.format("\t[Warn] Duplicate Error Item=(%s,%s=(%s))!", key, err, key));	
			}
			else
			{
				logger.error(String.format("\t[Error] Illegal System Result Input='%s'!\n", line));
				return;
			}
		}
		logger.debug(String.format("\t[Info] Analyzed System Result...(%d)", rtMap.size()));
		
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
		
		for(String id:idSet)
		{
			Map<String,Set<String>> gt = gtMap.get(id);
			Map<String,Set<String>> rt = rtMap.get(id);
			
			if(gt==null)
			{				
				throw new java.lang.Exception(String.format("Missing ID=%s in Ground Truth!", id));
			}
			else if(rt==null)
			{
				throw new java.lang.Exception(String.format("Missing ID=%s in Result!", id));
			}
			// NLPTEA15, additional position level
			//String gt_pos = gtMap.get(id).get(0);
			//String rt_pos = rtMap.get(id).get(0);
			if(IsCorrect(gt))
			{
				if(IsCorrect(rt))
				{
					dtnSet.add(id);
					itnSet.add(id);
					ptnSet.add(id);					
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
				if(IsCorrect(rt))
				{
					dfnSet.add(id);
					ifnSet.add(id);
					pfnSet.add(id);
				}
				else if(IDCheck(gt, rt))
				{
					dtpSet.add(id);
					itpSet.add(id);
					if(POSCheck(gt, rt))
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
		StringBuffer outputBuf = new StringBuffer("NLPTEA 2016 Shared Task"+Envset.BreakLine);
		outputBuf.append("Chinese Grammatical Error Diagnosis"+Envset.BreakLine+Envset.BreakLine);
		
		logger.debug(String.format("\t[Info] dfp=%d; dtn=%d; dfn=%d; dtp=%d\n", dfpSet.size(), dtnSet.size(), dfnSet.size(), dtpSet.size())); 
		logger.debug(String.format("\t[Info] ifp=%d; itn=%d; ifn=%d; itp=%d\n", ifpSet.size(), itnSet.size(), ifnSet.size(), itpSet.size())); 
		logger.debug(String.format("\t[Info] pfp=%d; ptn=%d; pfn=%d; ptp=%d\n", pfpSet.size(), ptnSet.size(), pfnSet.size(), ptpSet.size())); 
		double fp = ((double)dfpSet.size())/(dfpSet.size()+dtnSet.size());
		double daccuracy = ((double)dtpSet.size()+dtnSet.size())/idSet.size();
		double dprecision = ((double)dtpSet.size())/(dtpSet.size()+dfpSet.size());
		double drecall = ((double)dtpSet.size())/(dtpSet.size()+dfnSet.size());
		double df1Score = 2*(dprecision*drecall)/(dprecision+drecall);
		double iaccuracy = ((double)itpSet.size()+itnSet.size())/idSet.size();
		double iprecision = ((double)itpSet.size())/(itpSet.size()+ifpSet.size());
		double irecall = ((double)itpSet.size())/(itpSet.size()+ifnSet.size());
		double if1Score = 2*(iprecision*irecall)/(iprecision+irecall);
		// NLPTEA15, additional position level
		double paccuracy = ((double)ptpSet.size()+ptnSet.size())/idSet.size();
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
				                                                    idSet.size(),
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
                                                                    idSet.size(),
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
                                                                    idSet.size(),
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
			JList<String> ittpSet = new JList<String>(); ittpSet.addAll(itpSet); ittpSet.addAll(ifnSet);
			qsw.line(String.format("\t%s", OutputIL(ittpSet,rtMap, gtMap, ET.TP)));
			qsw.line("");
			qsw.line("\t#FP:");
			JList<String> itfpSet = new JList<String>(); itfpSet.addAll(ifpSet); itfpSet.addAll(ifnSet);
			qsw.line(String.format("\t%s", OutputIL(itfpSet,rtMap, gtMap, ET.FP)));
			qsw.line("");
			qsw.line("\t#TN:");
			qsw.line(String.format("\t%s", OutputIL(itnSet,rtMap, gtMap, ET.TN)));
			qsw.line("");
			qsw.line("\t#FN:");
			qsw.line(String.format("\t%s", OutputIL(ifnSet,rtMap, gtMap, ET.FN)));
			qsw.line("");
			// NLPTEA15, additional position level
			qsw.line("Position Level");
			qsw.line("");
			qsw.line("\t#TP:");
			JList<String> pttpSet = new JList<String>(); pttpSet.addAll(ptpSet); pttpSet.addAll(pfnSet);
			qsw.line(String.format("\t%s", OutputPL(pttpSet, rtMap, gtMap, ET.TP)));
			qsw.line("");
			qsw.line("\t#FP:");
			JList<String> ptfpSet = new JList<String>(); ptfpSet.addAll(pfpSet); ptfpSet.addAll(pfnSet);
			qsw.line(String.format("\t%s", OutputPL(ptfpSet, rtMap, gtMap, ET.FP)));
			qsw.line("");
			qsw.line("\t#TN:");
			qsw.line(String.format("\t%s", OutputPL(ptnSet, rtMap, gtMap, ET.TN)));
			qsw.line("");
			qsw.line("\t#FN:");
			qsw.line(String.format("\t%s", OutputPL(pfnSet, rtMap, gtMap, ET.FN)));
			qsw.line("");
			qsw.close();
		}
	}
}
