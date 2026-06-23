import { Toaster } from "sonner";
import LayoutContent from "./layoutComponent";


interface RootLayoutProps {
  children: React.ReactNode;
}

const RootLayout = ({ children }: RootLayoutProps) => {
    return (
        <>
            <LayoutContent>
              <Toaster position="top-right" richColors />
                {children}
            </LayoutContent>
        </>
    );
}

export default RootLayout;