import Header from "@/component/UI/header";
import Sidebar from "@/component/UI/sidebar";



interface LayoutContentProps {
    children: React.ReactNode;
}

const LayoutContent: React.FC<LayoutContentProps> = ({ children }) => {
    return (
        <div className="min-h-screen bg-gray-50 dark:bg-darkBackground transition-colors duration-300">
            <div className="flex min-h-screen">
                <aside className="w-0 lg:w-[300px] flex-shrink-0 bg-transparent transition-all duration-300">
                    <Sidebar />
                </aside>
                <section className="flex-1 flex flex-col min-h-screen w-full lg:w-[calc(100%-300px)] min-w-0">
                    <div className="flex-1 p-4">
                        <Header />
                    </div>
                    <main className="flex-1 p-3 sm:p-4 lg:p-6 xl:p-8 bg-gray-50 dark:bg-darkBackground transition-colors duration-300 min-w-0 overflow-x-hidden">
                        <div className="w-full max-w-none h-full">
                            <div>
                                {children}
                            </div>
                        </div>
                    </main>
                </section>
            </div>
        </div>
    );
}

export default LayoutContent;