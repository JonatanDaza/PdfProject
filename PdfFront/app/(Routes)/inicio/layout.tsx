import React from "react";
import LayoutContent from "./layoutComponent";


interface RootLayoutProps {
  children: React.ReactNode;
}

const RootLayout = ({ children }: RootLayoutProps) => {
    return (
        <>
            <LayoutContent>
                {children}
            </LayoutContent>
        </>
    );
}

export default RootLayout;