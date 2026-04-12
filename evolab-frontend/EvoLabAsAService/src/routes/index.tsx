import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import {
    createBrowserRouter,
    RouterProvider
} from "react-router-dom"
import { Layout } from '../components/layout/Layout'
import { Home } from '../pages/home/Home'
import {Register} from "../pages/Auth/Register.tsx";
import {Login} from "../pages/Auth/Login.tsx";

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

    ]
  }
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
)
