// Generated from CA.g4 by ANTLR 4.5
package beast.app.beauti.compactanalysis;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CAParser}.
 */
public interface CAListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link CAParser#casentence}.
	 * @param ctx the parse tree
	 */
	void enterCasentence(CAParser.CasentenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#casentence}.
	 * @param ctx the parse tree
	 */
	void exitCasentence(CAParser.CasentenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#template}.
	 * @param ctx the parse tree
	 */
	void enterTemplate(CAParser.TemplateContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#template}.
	 * @param ctx the parse tree
	 */
	void exitTemplate(CAParser.TemplateContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#templatename}.
	 * @param ctx the parse tree
	 */
	void enterTemplatename(CAParser.TemplatenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#templatename}.
	 * @param ctx the parse tree
	 */
	void exitTemplatename(CAParser.TemplatenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#subtemplate}.
	 * @param ctx the parse tree
	 */
	void enterSubtemplate(CAParser.SubtemplateContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#subtemplate}.
	 * @param ctx the parse tree
	 */
	void exitSubtemplate(CAParser.SubtemplateContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#idpattern}.
	 * @param ctx the parse tree
	 */
	void enterIdpattern(CAParser.IdpatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#idpattern}.
	 * @param ctx the parse tree
	 */
	void exitIdpattern(CAParser.IdpatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#key}.
	 * @param ctx the parse tree
	 */
	void enterKey(CAParser.KeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#key}.
	 * @param ctx the parse tree
	 */
	void exitKey(CAParser.KeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(CAParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(CAParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#import_}.
	 * @param ctx the parse tree
	 */
	void enterImport_(CAParser.Import_Context ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#import_}.
	 * @param ctx the parse tree
	 */
	void exitImport_(CAParser.Import_Context ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#filename}.
	 * @param ctx the parse tree
	 */
	void enterFilename(CAParser.FilenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#filename}.
	 * @param ctx the parse tree
	 */
	void exitFilename(CAParser.FilenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#alignmentprovider}.
	 * @param ctx the parse tree
	 */
	void enterAlignmentprovider(CAParser.AlignmentproviderContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#alignmentprovider}.
	 * @param ctx the parse tree
	 */
	void exitAlignmentprovider(CAParser.AlignmentproviderContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#arg}.
	 * @param ctx the parse tree
	 */
	void enterArg(CAParser.ArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#arg}.
	 * @param ctx the parse tree
	 */
	void exitArg(CAParser.ArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#partition}.
	 * @param ctx the parse tree
	 */
	void enterPartition(CAParser.PartitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#partition}.
	 * @param ctx the parse tree
	 */
	void exitPartition(CAParser.PartitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#partitionpattern}.
	 * @param ctx the parse tree
	 */
	void enterPartitionpattern(CAParser.PartitionpatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#partitionpattern}.
	 * @param ctx the parse tree
	 */
	void exitPartitionpattern(CAParser.PartitionpatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#link}.
	 * @param ctx the parse tree
	 */
	void enterLink(CAParser.LinkContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#link}.
	 * @param ctx the parse tree
	 */
	void exitLink(CAParser.LinkContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#unlink}.
	 * @param ctx the parse tree
	 */
	void enterUnlink(CAParser.UnlinkContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#unlink}.
	 * @param ctx the parse tree
	 */
	void exitUnlink(CAParser.UnlinkContext ctx);
	/**
	 * Enter a parse tree produced by {@link CAParser#set}.
	 * @param ctx the parse tree
	 */
	void enterSet(CAParser.SetContext ctx);
	/**
	 * Exit a parse tree produced by {@link CAParser#set}.
	 * @param ctx the parse tree
	 */
	void exitSet(CAParser.SetContext ctx);
}