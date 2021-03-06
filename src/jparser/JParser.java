package jparser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;

public class JParser {
	private ParserFactory factory;
	class UnitFilePair {
		public JCCompilationUnit unit;
		public String filename;
		public UnitFilePair(JCCompilationUnit _unit, String _filename){
			unit = _unit;
			filename = _filename;
		}
	}
	private List<UnitFilePair> units;
	
	public JParser() {
		Context context = new Context();
		JavacFileManager.preRegister(context);
		factory = ParserFactory.instance(context);		
		
		units = new ArrayList<UnitFilePair>();
	}

	public void parse(String filename) throws IOException {
//		String f = processConstructor(readFile(filename).toString());
		String f = readFile(filename).toString();
		Parser parser = factory.newParser(f, true, false, true);
		units.add(new UnitFilePair(parser.parseCompilationUnit(), filename));
	}
	
	public void parseDir(String d) throws IOException {
		File dir = new File(d);
		
		if (!dir.exists()) {
			return;
		}
		if (dir.isFile()) {
			String filename = dir.getName();
			String ext = filename.substring(filename.lastIndexOf(".")+1);
			if(ext.toLowerCase().equals("java"))
				parse(dir.getPath());
			return;
		}
		
		for(File x : dir.listFiles()){
			parseDir(x.getPath());
		}
	}

	public List<MethodNode> getMethodNodes() throws IOException {
		List<MethodNode> ret = new ArrayList<MethodNode>();
		ClassScanner scanner = new ClassScanner();
		
		for(UnitFilePair x : units) {
			int i = ret.size();
			scanner.visitCompilationUnit(x.unit, ret);
			while(i < ret.size()) {
				MethodNode m = ret.get(i++);
				m.filename = x.filename;
			}
		}
		
		return ret;
	}

	
	private class ClassScanner extends TreeScanner<List<MethodNode>, List<MethodNode>> {
		@Override
		public List<MethodNode> visitClass(ClassTree node, List<MethodNode> p) {
			for(Tree x : node.getMembers()) {
				if(x.getKind() == Tree.Kind.METHOD) {
					MethodNode m = new MethodNode((MethodTree) x);
					m.classname = node.getSimpleName().toString();
					if(((MethodTree)x).getName().toString().equals("<init>")){
						m.name = m.classname;
					} else {
						m.name = ((MethodTree)x).getName().toString();
					}
					p.add(m);
				}
				
				if(x.getKind() == Tree.Kind.CLASS) {
					List<MethodNode> tmp = new ArrayList<MethodNode>();
					ClassScanner scanner = new ClassScanner();
					scanner.visitClass((ClassTree)x, tmp);
					p.addAll(tmp);
				}
			}
			
			return p;
		}
	}
	
	private CharSequence readFile(String file) throws IOException {
		FileInputStream fin = new FileInputStream(file);
		FileChannel ch = fin.getChannel();
		ByteBuffer buffer = ch.map(MapMode.READ_ONLY, 0, ch.size());
		fin.close();
		return Charset.defaultCharset().decode(buffer);
	}
}
