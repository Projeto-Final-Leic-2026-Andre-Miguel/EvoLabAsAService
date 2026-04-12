import { Outlet } from "react-router-dom";
import { TopBar } from "./TopBar";

export function Layout() {
    return (
        <>
            <TopBar />
            <main>
                <Outlet />
            </main>
        </>
    );
}
