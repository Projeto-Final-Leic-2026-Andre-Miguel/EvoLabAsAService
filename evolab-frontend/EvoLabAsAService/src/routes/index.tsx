import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import {
    createBrowserRouter,
    RouterProvider
} from "react-router-dom"
import { Layout } from '../components/layout/Layout'
import { Home } from '../pages/home/Home'
import {Register} from "../pages/auth/Register.tsx";
import {Login} from "../pages/auth/Login.tsx";
import {AuthProvider} from "../contexts/AuthContext";
import {Profile} from "../pages/profile/Profile";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Layout />,
    children: [
        { index: true, element: <Home /> },
      //  { path: "projects", element: <Projects /> }, // Injetado no Outlet em /projects
      //  { path: "credentials", element: <Credentials /> }, // Injetado no Outlet em /credentials
      //  { path: "contact", element: <Contact /> }, // Injetado no Outlet em /contact
          {path : "register", element: <Register />,},
          {path : "login", element: <Login />},
          {path : "profile", element: <Profile />},
    ]
  }
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
        <RouterProvider router={router} />
    </AuthProvider>
  </StrictMode>,
)
