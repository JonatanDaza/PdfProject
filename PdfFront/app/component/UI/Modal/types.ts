import type { ReactNode } from "react";

export type ModalSize = 'sm' | 'md' | 'lg' | 'xl' | 'xxl' | 'xxxl' | "full";

export interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title?: ReactNode;
    size?: ModalSize;
    children: ReactNode;
    className?: string;
}