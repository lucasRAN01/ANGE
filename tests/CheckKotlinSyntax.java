import java.nio.file.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.*;
import org.jetbrains.kotlin.cli.jvm.compiler.*;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.*;
public class CheckKotlinSyntax {
  static int errors=0;
  static void walk(PsiElement e) {
    if(e instanceof PsiErrorElement) { System.out.println(((PsiErrorElement)e).getErrorDescription()+" at "+e.getTextOffset()); errors++; }
    for(PsiElement c:e.getChildren())walk(c);
  }
  public static void main(String[] args)throws Exception {
    Disposable d=Disposer.newDisposable();
    try {
      KotlinCoreEnvironment env=KotlinCoreEnvironment.createForProduction(d,new CompilerConfiguration(),EnvironmentConfigFiles.JVM_CONFIG_FILES);
      for(String file:args) { KtFile f=new KtPsiFactory(env.getProject(),false).createFile(Path.of(file).getFileName().toString(),Files.readString(Path.of(file)).replace("\r\n","\n")); walk(f); System.out.println(file+": parsed"); }
    }finally { Disposer.dispose(d); }
    if(errors>0)System.exit(1);
    System.out.println("PASS syntax only; no dependency resolution or Android type checking.");
  }
}
