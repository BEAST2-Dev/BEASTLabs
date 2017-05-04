// Generated from CA.g4 by ANTLR 4.5
package beast.app.beauti.compactanalysis;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CAParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface CAVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link CAParser#casentence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCasentence(CAParser.CasentenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#template}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplate(CAParser.TemplateContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#templatename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplatename(CAParser.TemplatenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#subtemplate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtemplate(CAParser.SubtemplateContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#idpattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdpattern(CAParser.IdpatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey(CAParser.KeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(CAParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#import_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_(CAParser.Import_Context ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#filename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilename(CAParser.FilenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#alignmentprovider}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlignmentprovider(CAParser.AlignmentproviderContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArg(CAParser.ArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition(CAParser.PartitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#partitionpattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionpattern(CAParser.PartitionpatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#link}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLink(CAParser.LinkContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#unlink}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnlink(CAParser.UnlinkContext ctx);
	/**
	 * Visit a parse tree produced by {@link CAParser#set}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet(CAParser.SetContext ctx);
}