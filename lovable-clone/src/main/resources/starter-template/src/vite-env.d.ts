/// <reference types="vite/types/importMeta.d.ts" />
declare module "*.svg" {
    import React from "react";

    // React component import
    export const ReactComponent: React.FC<React.SVGProps<SVGSVGElement>>;

    // URL import for <img />
    const src: string;
    export default src;
}
