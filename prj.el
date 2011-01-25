(jde-project-file-version "1.0")

;; Reset class path and source path
(jde-set-variables
 '(jde-project-name "openalarm-android")
 '(jde-gen-buffer-boilerplate
   '("/**"
     "*  OpenAlarm - an extensible alarm for Android"
     "*  Copyright (C) 2010 Liu Yen-Liang (Josh)"
     "*"
     "*"
     "*  This program is free software: you can redistribute it and/or modify"
     "*  it under the terms of the GNU General Public License as published by"
     "*  the Free Software Foundation, either version 3 of the License, or"
     "*  (at your option) any later version."
     "*"
     "*  This program is distributed in the hope that it will be useful,"
     "*  but WITHOUT ANY WARRANTY; without even the implied warranty of"
     "*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the"
     "*  GNU General Public License for more details."
     "*"
     "*  You should have received a copy of the GNU General Public License"
     "*  along with this program. If not, see <http://www.gnu.org/licenses/>."
     "*/"))
 '(jde-log-max 5000)
 '(jde-enable-abbrev-mode t)
 ;; Class path for browsing files and generate code templates
 '(jde-global-classpath
   (quote ("$ANDROID_SDK_ROOT/platforms/android-7/android.jar"
           "$HOME/projects/openalarm-android/bin")))
 '(jde-sourcepath
   (quote ("$HOME/projects/openalarm-android/src"
           "/Volumes/Leo21/eclair_21/frameworks/base/core/java")
          ))
 '(jde-compile-option-directory "$HOME/projects/openalarm-android/bin") ;;编译生成的class文件放在这个目录，就是项目的bin目录
 '(jde-complete-function (quote jde-complete-menu))  ;;代码提示的方式 使用菜单在下面我把快捷键设成了Ctrl+return
 '(jde-run-working-directory "$HOME/projects/openalarm-android/bin") ;;运行命令以此目录为基目录(,,才能正确指定哪些文件夹对应着java中的包名)
 '(jde-help-docsets
   (quote (("Android SDK Doc" "$ANDROID_SDK_ROOT/docs/index.html" nil))));;指定jdk帮助文档的位置,当然也可以加入其他的帮助文档
 '(jde-build-function (quote (jde-ant-build)));; 使用ant 构建项目，默认是make
 '(jde-ant-args "-emacs") ; compile output in emacs format ,这个不知有没有用，先放这
 '(jde-ant-complete-target t)
 '(jde-ant-enable-find t)  ;;如果当前目录没有build.xml会搜索子目录
 '(jde-ant-read-args nil) ;; ant脚本可能要传入一些参数，jde会要求用户输入，这里禁用之
 '(jde-ant-read-buildfile nil);; 不要求用户输入build文件具体的名字，使用默认的build.xml
 '(jde-ant-read-target nil);; 不要求用户输入调用哪个target ,即调用build.xml文件中默认的target
 '(jde-ant-use-global-classpath nil);;使用jde的 global-classpath
 '(jde-ant-working-directory "$HOME/projects/openalarm-openalarm/bin/") ;;指定ant 的工作目录，指定为bin对应目录(注意目录必须以"/"结尾，Windows上以"\"结尾  ) ;;这里没进行详细判断pro_bin_dir是不是以"/"结尾，直接加在末尾加一个"/" ,linux上可以理解"/home//jixiuf//" 这样不太合法的目录

;;;关于cross reference ，应该是记录某个方法在哪些地方被调用
 ;;我们在一个方法上按"C-c C-v a"会运行jde-xref-first-caller命令，
 ;;然后就后跳到第一个调用此方法的地方（如果有的话）然后"C-c C-v n" jde-xref-next-caller，则一直next
 ;;而运行 jde-xref-display-call-tree 则会以树状展示调用关系
 ;; M-x jde-xref-list-uncalled-functions,则展示当前文件，哪些方法没被调用过
 '(jde-built-class-path (quote ("$HOME/projects/openalarm-android/bin")))
 ;;Similar to `jde-global-classpath', except this path should only
 ;;have those places where compile files live.  This list of paths could
 ;;contain both directories and jar files.  Each of these should
 ;;correspond to the root of the build tree, in other words the
 ;;directories under it should correpond to packages.
; '(jde-xref-cache-size 3);;设定缓存大小，默认为3 ，越大越快越大越占内存
; '(jde-xref-db-base-directory proj_root_dir);;运行jde-xref-make-xref-db会生成一些索引文件，放在proj_root_dir/xrefdb目录下
; '(jde-xref-store-prefixes (quote ("mail")));;只索引这个包里的
 ;; M-x jde-xref-make-xref-db  使用此命令生成相应的文件
; (run-at-time "11:00pm" 86400 'jde-xref-make-xref-db);;从11:00pm开始每隔86400s更新一下索引目录./xrefdb/
 ;;另外一个办法就是每次调用compile后调用一下jde-xref-update命令，,现在还不清楚相应的hook是什么，所以暂时没实现

 ;;关于etag jtags   Tagging Java Source Code，这个应该就是eclipse调试时的代码跳转
 ;;比如光标在"abb".substring() ,然后点击"Alt+." 就会跳到String.substring()方法的定义处
 ;;前提是你将jdk的src.zip 解压后的目录加入到jde-sourcepath中，用我的add_src_path()可以做到，另外一点是
 ;;在源代码目录下运行jtags命令，(它是etags命令的一个包装，所以确保你装了etags )
 ;;使用方法，当然先那建索引库jdee提供了jtags命令用于扫描java源文件，方法在bash 中进入到项目根目录然后运行jtags命令，会在根目录下生成一个TAGS文件
 ;;所有你想跳转过去的源代码都要如此处理
 )

(jde-wiz-update-class-list)
