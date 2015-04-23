#<center>《How To Make Tomcat》笔记摘要
---------------
Step by step, follow 《How Tomcat Works》! 暂时基于Tomcat4，后续升级。
##<center style="font-family:Microsoft Yahei">第二章	简单的Servlet容器

####1. javax.servelt.Servlet接口
1. servlet编程基本基于下面两个包的接口和类：javax.servlet和javax.servlet.http，这中间最重要的就是接口javax.servlet.Servlet。所有的servlet都必须实现该接口或继承实现了该接口的类。
2. 接口主要有5个方法：

		public void init(ServletConfig config) throws ServletException;
		public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException;
		public void destroy();
		public ServletConfig getServletConfig();
		public String getServletInfo();

	前面三个方法属于Servlet生命周期方法。

	init在Servlet容器创建Servlet实例之后被调用，该方法必须被容器明确调用一次来将Servlet实例纳入到服务列表（在Servlet接受任何请求之前完成），当然方法内我们也可以添加其他内容，比如初始化一个数据库连接等。一般情况下，left blank（原文）即可。
	
	service在任何请求到来时被调用，该方法需要一个ServletRequest接口实现对象和一个ServletResponse接口实现对象，前者携带了客户端HTTP请求信息，后者则封装了servlet的响应信息。service方法会被调用任意多次。

	destroy方法调用——在容器移除servlet实例之前（通常是servlet容器被关闭或者容器需要释放一些空间），即调用条件：用到这个servlet服务的所有线程均退出或者超时。当此方法被调用后，service方法就不会再被调用。destroy方法给servlet提供了一个释放资源（比如内存，文件句柄持有，线程占用等）的途径，并且保证了servlet当前状态和持久化状态一致（即当前状态正确持久化）。
####2. servlet容器做的事情：

1. 当servlet第一次被调用，容器加载servlet类并调用init方法（once only）；
2. 容器为每一个请求（request）提供一个ServletRequest和ServletResponse实现；
3. 将2作为参数调用servlet的service方法；
4. 当servlet被关闭的时候，调用其destroy方法并且卸载servlet类。
####3. 关于ch02代码的问题

1. HttpServer1和ServletProcessor1出现的问题：由于传入servlet.service方法的参数是经过上转型的实际为Request和Response的对象，则会导致知晓servlet细节的人将这两个参数下转型回Request和Response，这样就可以调用其公有方法（如parse)，这违背了安全原则（不能在service方法中暴露ServletRequest接口定义之外的方法）。当然，不能将所有敏感方法设置为private（别的类不能调用），一种方法是设置为默认访问控制权限；更好的方法是使用facade模式来设计。
2. facade模式（基于本例）：如RequestFacade，继承ServletRequest接口，并持有一个私有Request对象，通过构造函数方式置入。所有接口方法中通过调用该Request对象来完成，然后servlet的service方法传入参数RequestFacade和ResponseFacade,这样调用者只能使用接口中定义的方法，达到了隔离的目的！
3. 代码见ch01中HttpServer2及ServletProccessor2

##<center style="font-family:Microsoft Yahei">第三章	Connetor——连接器

####1. StringManager为每个包提供一个单例，不同包名对应实例包含在static hashTable（多线程）中。StringManager可读取包中Properties文件，并用于修改——该文件可存储包类需要用到的error messages配置。
####2. 第二点
####3. 第三点
####4. 第四点
